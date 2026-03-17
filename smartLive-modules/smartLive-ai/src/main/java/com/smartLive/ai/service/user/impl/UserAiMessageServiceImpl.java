package com.smartLive.ai.service.user.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.UserAiMessage;
import com.smartLive.ai.domain.UserAiSession;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.mapper.UserAiMessageMapper;
import com.smartLive.ai.service.chat.AgentChatContext;
import com.smartLive.ai.service.user.IUserAiMessageService;
import com.smartLive.ai.service.user.IUserAiSessionService;
import com.smartLive.ai.service.user.support.MessageTableChatMemoryManager;
import com.smartLive.ai.service.user.support.RecommendationCardHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 用户 AI 消息服务实现类。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserAiMessageServiceImpl extends ServiceImpl<UserAiMessageMapper, UserAiMessage> implements IUserAiMessageService {

    private final IUserAiSessionService sessionService;
    private final AgentChatContext agentChatContext;
    private final MessageTableChatMemoryManager chatMemoryManager;
    private final RecommendationCardHelper recommendationCardHelper;

    /**
     * 查询消息列表。
     */
    @Override
    public List<UserAiMessage> selectMessageList(Integer current, Long sessionId) {
        List<UserAiMessage> list = query().eq("session_id", sessionId)
                .orderByDesc("create_time")
                .page(new Page<>(current, 10))
                .getRecords();
        Collections.reverse(list);
        return list;
    }

    /**
     * 保存消息。
     */
    @Override
    public UserAiMessage saveMessage(Long sessionId, String role, String content) {
        UserAiMessage message = new UserAiMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setType("text");
        message.setCreateTime(new Date());
        save(message);

        UserAiSession session = new UserAiSession();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        sessionService.updateById(session);

        return message;
    }

    /**
     * 处理聊天服务端事件流。
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
        chatMemoryManager.rebuildConversationMemory(conversationId, sessionId, contextEnabled);

        UserAiMessage userRecord = saveMessage(sessionId, "user", message);
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
                    log.info("Recommendation JSON detected, emitting card_render event");
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
                        UserAiMessage assistantRecord = saveMessage(sessionId, "assistant", responseText);
                        chatMemoryManager.appendMessageToCache(assistantRecord);
                        log.info("AI reply persisted, sessionId={}", sessionId);
                    } catch (Exception e) {
                        log.error("Failed to persist AI reply", e);
                    }
                });
    }

    /**
     * 解析会话 ID。
     */
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

    /**
     * 判断文本是否存在。
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
