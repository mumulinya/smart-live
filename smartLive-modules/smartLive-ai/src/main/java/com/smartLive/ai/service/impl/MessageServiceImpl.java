package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.domain.Session;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.mapper.MessageMapper;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.ai.service.chat.AgentChatContext;
import com.smartLive.ai.service.chat.support.MessageTableChatMemoryManager;
import com.smartLive.ai.service.chat.support.RecommendationCardHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * AI 消息业务实现类
 * 
 * 核心流程：
 * 1. 聊天入口会根据会话 ID 重建 ChatMemory（优先从 Redis 获取，缺失则回源数据库）
 * 2. 实时保存用户发送的消息及 AI 生成的消息到 message 数据表
 * 3. 异步维护 Redis 中的消息分片缓存，优化上下文窗口加载性能
 * 4. 内置推荐卡片识别逻辑，通过 card_render 事件推送给前端进行 UI 渲染
 *
 * @author smartLive
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    private final ISessionService sessionService;
    private final AgentChatContext agentChatContext;
    private final MessageTableChatMemoryManager chatMemoryManager;
    private final RecommendationCardHelper recommendationCardHelper;

    /**
     * 分页查询会话详情中的消息列表
     * 自动处理列表反转，保证时间轴从旧到新展示
     *
     * @param current 当前页
     * @param sessionId 会话 ID
     * @return 排序好的消息列表
     */
    @Override
    public List<Message> selectMessageList(Integer current, Long sessionId) {
        List<Message> list = query().eq("session_id", sessionId)
                .orderByDesc("create_time")
                .page(new Page<>(current, 10))
                .getRecords();
        Collections.reverse(list);
        return list;
    }

    /**
     * 保存单条消息记录并更新会话活跃时间
     *
     * @param sessionId 会话 ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     * @return 已持久化的消息对象
     */
    @Override
    public Message saveMessage(Long sessionId, String role, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setType("text");
        message.setCreateTime(new Date());
        this.save(message);

        Session session = new Session();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        sessionService.updateById(session);

        return message;
    }

    /**
     * AI 对话核心流式接口
     * 
     * 1. 组装 AI 请求 DTO (AIChatRequest)
     * 2. 重建历史记忆 (ChatMemory)
     * 3. 流式调用大模型，实时通过 SSE 推送给前端
     * 4. 识别并提取流中的 JSON 文本，转化为推荐卡片事件
     * 5. 对话结束后异步持久化助手回答
     *
     * @param messageDTO 原始请求参数
     * @return SSE 事件流
     */
    @Override
    public Flux<ServerSentEvent<String>> chat(MessageDTO messageDTO) {
        Long sessionId = messageDTO.getSessionId();
        Long userId = messageDTO.getUserId();
        String message = messageDTO.getMessage();

        AIChatRequest request = new AIChatRequest();
        request.setMessage(message);
        if (sessionId != null) {
            request.setSessionId(String.valueOf(sessionId));
        }
        if (userId != null) {
            request.setUserId(String.valueOf(userId));
        }
        request.setX(messageDTO.getX());
        request.setY(messageDTO.getY());
        request.setDistrict(messageDTO.getRegion());

        String conversationId = resolveConversationId(request);
        boolean contextEnabled = !Boolean.FALSE.equals(messageDTO.getContextMode());
        // 在写入当前轮用户消息前重建记忆，避免“历史 + 当前输入”重复注入。
        chatMemoryManager.rebuildConversationMemory(conversationId, sessionId, contextEnabled);

        Message userRecord = saveMessage(sessionId, "user", message);
        chatMemoryManager.appendMessageToCache(userRecord);

        StringBuilder fullResponse = new StringBuilder();
        return agentChatContext.generateResponse(request, false)
                .map(chunk -> {
                    String safeChunk = chunk == null ? "" : chunk;
                    fullResponse.append(safeChunk);
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(safeChunk)
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    String response = fullResponse.toString();
                    String json = recommendationCardHelper.extractRecommendationJson(response);
                    if (json == null) {
                        return Flux.empty();
                    }
                    String normalizedJson = recommendationCardHelper.normalizeRecommendationJson(json);
                    log.info("检测到推荐 JSON，发送 card_render 卡片事件");
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("card_render")
                            .data(normalizedJson)
                            .build());
                }))
                .doFinally(signalType -> {
                    String responseText = fullResponse.toString();
                    if (responseText.isEmpty()) {
                        return;
                    }
                    try {
                        // 模型最终返回文本仍按业务方式落库，同时增量刷新 Redis 记忆缓存。
                        Message assistantRecord = saveMessage(sessionId, "assistant", responseText);
                        chatMemoryManager.appendMessageToCache(assistantRecord);
                        log.info("已保存会话 {} 的助手回答", sessionId);
                    }
                    catch (Exception e) {
                        log.error("保存助手回答失败", e);
                    }
                });
    }

    private String resolveConversationId(AIChatRequest request) {
        if (request == null) {
            return "anonymous";
        }
        if (hasText(request.getSessionId())) {
            return request.getSessionId();
        }
        if (hasText(request.getUserId())) {
            return "user::" + request.getUserId();
        }
        return "anonymous";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
