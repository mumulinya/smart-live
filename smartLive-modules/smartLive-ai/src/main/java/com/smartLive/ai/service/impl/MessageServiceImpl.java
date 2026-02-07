package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.domain.Session;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.mapper.MessageMapper;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.ai.service.orchestration.AIChatOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * AI Message Service Implementation
 *
 * @author smartLive
 */
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private AIChatOrchestrator aiChatOrchestrator;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<Message> selectMessageList(Integer current,Long sessionId) {
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
        message.setType("text"); // Default type
        message.setCreateTime(new Date());
        this.save(message);

        // Update session update_time
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

        // 1. 保存用户消息
        saveMessage(sessionId, "user", message);

        // 2. 构建请求
        AIChatRequest request = new AIChatRequest();
        request.setMessage(message);
        request.setSessionId(String.valueOf(sessionId));
        request.setUserId(String.valueOf(userId));
        request.setX(messageDTO.getX());
        request.setY(messageDTO.getY());
        request.setDistrict(messageDTO.getRegion());
        // 3. 累积完整响应用于保存
        StringBuilder fullResponse = new StringBuilder();

        // 4. 处理AI响应流 - 流式发送文本，最后检测并发送card_render
        return aiChatOrchestrator.processMessage(request)
                .map(chunk -> {
                    fullResponse.append(chunk);
                    // 流式发送普通文本消息
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(chunk)
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    // 流结束后，检查完整响应是否包含推荐JSON，如果有则发送card_render事件
                    String response = fullResponse.toString();
                    String json = extractRecommendationJson(response);
                    if (json != null) {
                        log.info("📦 Detected recommendation JSON, sending card_render event");
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("card_render")
                                .data(json)
                                .build());
                    }
                    return Flux.empty();
                }))
                .doFinally(signalType -> {
                    // 保存完整的助手消息
                    String responseText = fullResponse.toString();
                    if (!responseText.isEmpty()) {
                        try {
                            saveMessage(sessionId, "assistant", responseText);
                            log.info("✅ Saved assistant response for session {}", sessionId);
                        } catch (Exception e) {
                            log.error("❌ Failed to save assistant response", e);
                        }
                    }
                });
    }

    /**
     * 从完整响应中提取推荐JSON
     * AI返回格式通常是: 文本内容 + JSON对象
     */
    private String extractRecommendationJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        // 查找最后一个完整的JSON对象 (从最后的}往前找匹配的{)
        int end = content.lastIndexOf("}");
        if (end < 0) {
            return null;
        }

        // 从end往前找匹配的{，需要处理嵌套
        int braceCount = 0;
        int start = -1;
        for (int i = end; i >= 0; i--) {
            char c = content.charAt(i);
            if (c == '}') {
                braceCount++;
            } else if (c == '{') {
                braceCount--;
                if (braceCount == 0) {
                    start = i;
                    break;
                }
            }
        }

        if (start < 0) {
            return null;
        }

        String json = content.substring(start, end + 1);
        if (isShopRecommendationJson(json)) {
            return json;
        }
        return null;
    }

    /**
     * 判断是否为店铺推荐的JSON格式
     */
    private boolean isShopRecommendationJson(String content) {
        if (!content.startsWith("{") || !content.endsWith("}")) {
            return false;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            // 检查是否包含recommendations字段
            return jsonNode.has("recommendations") && jsonNode.has("replyText");
        } catch (Exception e) {
            return false;
        }
    }
}

