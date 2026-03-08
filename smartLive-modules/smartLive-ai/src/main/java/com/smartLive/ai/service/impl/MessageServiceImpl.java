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

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * 聊天入口：
 * 1. 先按会话重建 ChatMemory（优先 Redis，miss 再查 DB）
 * 2. 当前轮 user/assistant 消息继续由业务层写入 message 表
 * 3. 每次落库后同步追加到 Redis 记忆缓存，降低后续回源成本
 * 4. 推荐卡片额外透出 card_render 事件给前端
 */
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    private final ISessionService sessionService;
    private final AgentChatContext agentChatContext;
    private final MessageTableChatMemoryManager chatMemoryManager;
    private final RecommendationCardHelper recommendationCardHelper;

    @Override
    public List<Message> selectMessageList(Integer current, Long sessionId) {
        List<Message> list = query().eq("session_id", sessionId)
                .orderByDesc("create_time")
                .page(new Page<>(current, 10))
                .getRecords();
        Collections.reverse(list);
        return list;
    }

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
                    log.info("Detected recommendation JSON, sending card_render event");
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
                        log.info("Saved assistant response for session {}", sessionId);
                    }
                    catch (Exception e) {
                        log.error("Failed to save assistant response", e);
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
