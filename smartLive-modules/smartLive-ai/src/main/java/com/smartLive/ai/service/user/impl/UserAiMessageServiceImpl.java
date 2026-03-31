package com.smartLive.ai.service.user.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.UserAiMessage;
import com.smartLive.ai.domain.UserAiSession;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.mapper.UserAiMessageMapper;
import com.smartLive.ai.service.user.IUserAiMessageService;
import com.smartLive.ai.service.user.IUserAiSessionService;
import com.smartLive.ai.service.user.support.MessageTableChatMemoryManager;
import com.smartLive.ai.service.user.support.RecommendationCardHelper;
import com.smartLive.ai.service.user.support.StructuredToolCaptureRegistry;
import com.smartLive.ai.service.user.support.UserStructuredResponseService;
import com.smartLive.ai.strategy.user.AgentChatContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用户 AI 消息服务实现类。
 */
@Service
@Slf4j
public class UserAiMessageServiceImpl extends ServiceImpl<UserAiMessageMapper, UserAiMessage> implements IUserAiMessageService {

    private final IUserAiSessionService sessionService;
    private final AgentChatContext agentChatContext;
    private final MessageTableChatMemoryManager chatMemoryManager;
    private final RecommendationCardHelper recommendationCardHelper;
    private final UserStructuredResponseService userStructuredResponseService;
    private final StructuredToolCaptureRegistry structuredToolCaptureRegistry;
    private final ChatClient structuredRetryChatClient;

    public UserAiMessageServiceImpl(
            IUserAiSessionService sessionService,
            AgentChatContext agentChatContext,
            MessageTableChatMemoryManager chatMemoryManager,
            RecommendationCardHelper recommendationCardHelper,
            UserStructuredResponseService userStructuredResponseService,
            StructuredToolCaptureRegistry structuredToolCaptureRegistry,
            @Qualifier("structuredRetryChatClient") ChatClient structuredRetryChatClient
    ) {
        this.sessionService = sessionService;
        this.agentChatContext = agentChatContext;
        this.chatMemoryManager = chatMemoryManager;
        this.recommendationCardHelper = recommendationCardHelper;
        this.userStructuredResponseService = userStructuredResponseService;
        this.structuredToolCaptureRegistry = structuredToolCaptureRegistry;
        this.structuredRetryChatClient = structuredRetryChatClient;
    }

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

        String captureId = structuredToolCaptureRegistry.openCapture();
        attachStructuredCapture(request, captureId);

        UserAiMessage userRecord = saveMessage(sessionId, "user", message);
        chatMemoryManager.appendMessageToCache(userRecord);

        AtomicReference<String> responseToPersist = new AtomicReference<>();
        return streamAgentResponse(request, message, captureId, responseToPersist)
                .doFinally(signalType -> {
                    structuredToolCaptureRegistry.clearCapture(captureId);
                    persistAssistantResponse(sessionId, responseToPersist.get());
                });
    }

    /**
     * 使用现有 Agent 主链路返回流式结果。
     */
    private Flux<ServerSentEvent<String>> streamAgentResponse(
            AIChatRequest request,
            String userMessage,
            String captureId,
            AtomicReference<String> responseToPersist
    ) {
        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean suppressStructuredMessage = new AtomicBoolean(false);
        AtomicBoolean emittedVisibleMessage = new AtomicBoolean(false);
        return agentChatContext.generateResponse(request, false)
                .handle((String chunk, reactor.core.publisher.SynchronousSink<ServerSentEvent<String>> sink) -> {
                    String safeChunk = chunk == null ? "" : chunk;
                    fullResponse.append(safeChunk);

                    if (!suppressStructuredMessage.get()
                            && !emittedVisibleMessage.get()
                            && recommendationCardHelper.hasStructuredJsonStarted(fullResponse.toString())) {
                        suppressStructuredMessage.set(true);
                        return;
                    }

                    if (suppressStructuredMessage.get()) {
                        return;
                    }

                    if (!emittedVisibleMessage.get() && safeChunk.isBlank()) {
                        return;
                    }

                    sink.next(ServerSentEvent.<String>builder()
                            .event("message")
                            .data(safeChunk)
                            .build());

                    if (!safeChunk.isBlank()) {
                        emittedVisibleMessage.set(true);
                    }
                })
                .concatWith(Flux.defer(() -> finalizeResponse(
                        captureId,
                        fullResponse.toString(),
                        userMessage,
                        suppressStructuredMessage.get(),
                        emittedVisibleMessage.get(),
                        responseToPersist
                )))
                .doFinally(signalType -> {
                    if (!hasText(responseToPersist.get()) && fullResponse.length() > 0) {
                        responseToPersist.set(fullResponse.toString());
                    }
                });
    }

    /**
     * 持久化助手回复。
     */
    private void persistAssistantResponse(Long sessionId, String responseText) {
        if (!hasText(responseText)) {
            return;
        }
        try {
            UserAiMessage assistantRecord = saveMessage(sessionId, "assistant", responseText);
            chatMemoryManager.appendMessageToCache(assistantRecord);
            log.info("AI reply persisted, sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("Failed to persist AI reply", e);
        }
    }

    /**
     * 结束流式输出后，决定发结构化事件、修复重试或文本回退。
     */
    private Flux<ServerSentEvent<String>> finalizeResponse(
            String captureId,
            String response,
            String userMessage,
            boolean suppressStructuredMessage,
            boolean emittedVisibleMessage,
            AtomicReference<String> responseToPersist
    ) {
        String structuredJson = userStructuredResponseService.buildStructuredJsonFromCapture(captureId, userMessage, response);
        boolean shouldEmitStructuredCard = hasText(structuredJson) && (suppressStructuredMessage || !emittedVisibleMessage);

        if (shouldEmitStructuredCard) {
            responseToPersist.set(structuredJson);
            log.info("Structured JSON detected, emitting card_render event");
            return Flux.just(buildSseEvent("card_render", structuredJson));
        }

        if (!suppressStructuredMessage || response.isBlank()) {
            return Flux.empty();
        }

        return Mono.fromCallable(() -> retryStructuredResponse(captureId, userMessage, response))
                .subscribeOn(Schedulers.boundedElastic())
                .defaultIfEmpty("")
                .flatMapMany(retriedResponse -> {
                    String retriedJson = userStructuredResponseService.buildStructuredJsonFromCapture(captureId, userMessage, retriedResponse);
                    if (hasText(retriedJson)) {
                        responseToPersist.set(retriedJson);
                        log.info("Structured JSON retry succeeded, emitting card_render event");
                        return Flux.just(buildSseEvent("card_render", retriedJson));
                    }

                    responseToPersist.set(response);
                    log.warn("Structured response parsing failed after retry, fallback to final message event");
                    return Flux.just(buildSseEvent("message", response));
                });
    }

    /**
     * 结构化输出重试，一次失败后返回 null。
     */
    private String retryStructuredResponse(String captureId, String userMessage, String response) {
        String validationFailure = recommendationCardHelper.describeStructuredValidationFailure(response);
        String retryToolContext = userStructuredResponseService.buildRetryToolContext(captureId);
        try {
            return structuredRetryChatClient.prompt()
                    .user(buildStructuredRetryPrompt(userMessage, response, validationFailure, retryToolContext))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Structured JSON retry call failed", e);
            return null;
        }
    }

    /**
     * 构建结构化 JSON 重试提示词。
     */
    private String buildStructuredRetryPrompt(
            String userMessage,
            String response,
            String validationFailure,
            String retryToolContext
    ) {
        return """
                上一轮结构化输出校验失败，请根据用户问题、原始输出和错误原因，重新输出一个最终 JSON 对象。

                用户原始问题：
                %s

                原始模型输出：
                %s

                校验失败原因：
                %s

                本次请求已缓存的工具真实结果（直接来自 Tool 返回，请优先以此为准）：
                %s

                请注意：
                1. 只能输出一个最终 JSON 对象。
                2. 不要输出 markdown。
                3. 不要添加解释。
                4. 如果上面的工具真实结果里已经给出候选列表，shop/product 场景只能从这些候选 id 中选择 selectedIds。
                5. 如果候选列表为空，shop/product 必须返回 type + selectedIds + replyText，且 selectedIds 必须是空数组。
                6. order 场景必须严格以工具真实结果里的 success / orderId / message 为准；orderId 必须是字符串。
                7. shop/product 场景优先返回 type + selectedIds + replyText。
                8. 这是一次重新输出，不是对原字符串做解释，也不要重新发明新的候选数据。
                """.formatted(
                safePromptText(userMessage),
                safePromptText(response),
                safePromptText(validationFailure),
                safePromptText(retryToolContext)
        );
    }

    /**
     * 构建 SSE 事件。
     */
    private ServerSentEvent<String> buildSseEvent(String event, String data) {
        return ServerSentEvent.<String>builder()
                .event(event)
                .data(data)
                .build();
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

    /**
     * 规整提示词内容，避免 null 进入格式化文本。
     */
    private String safePromptText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 为当前请求附加结构化工具结果捕获 id。
     */
    private void attachStructuredCapture(AIChatRequest request, String captureId) {
        if (request == null || captureId == null) {
            return;
        }
        Map<String, Object> context = request.getContext() == null
                ? new HashMap<>()
                : new HashMap<>(request.getContext());
        context.put(StructuredToolCaptureRegistry.REQUEST_ID_KEY, captureId);
        request.setContext(context);
    }
}
