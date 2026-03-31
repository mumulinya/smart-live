package com.smartLive.ai.strategy.user.impl;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.strategy.user.AgentChatStrategy;
import com.smartLive.ai.strategy.user.support.AgentRouter;
import com.smartLive.ai.strategy.user.support.AgentRoutingDecision;
import com.smartLive.ai.strategy.user.support.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 直接路由策略类。
 */
@Slf4j
@Service
public class DirectRoutingStrategy implements AgentChatStrategy {

    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    private static final String FALLBACK_MESSAGE = "Sorry, the service is busy. Please try again later.";

    private final AgentRouter agentRouter;
    private final ChatClient generalChatClient;
    private final ChatClient shopAgentChatClient;
    private final ChatClient productAgentChatClient;
    private final ChatClient reviewAgentChatClient;

    public DirectRoutingStrategy(
            AgentRouter agentRouter,
            @Qualifier("generalChatClient") ChatClient generalChatClient,
            @Qualifier("shopAgentChatClient") ChatClient shopAgentChatClient,
            @Qualifier("productAgentChatClient") ChatClient productAgentChatClient,
            @Qualifier("reviewAgentChatClient") ChatClient reviewAgentChatClient
    ) {
        this.agentRouter = agentRouter;
        this.generalChatClient = generalChatClient;
        this.shopAgentChatClient = shopAgentChatClient;
        this.productAgentChatClient = productAgentChatClient;
        this.reviewAgentChatClient = reviewAgentChatClient;
    }

    /**
     * 返回字符串数据流。
     */
    @Override
    public Flux<String> streamChat(AIChatRequest request) {
        String chatId = resolveChatId(request);
        AgentRoutingDecision decision = agentRouter.routeDecision(request.getMessage());
        String enrichedMessage = buildEnrichedMessage(request);
        Map<String, Object> toolContext = buildToolContext(request, chatId);

        if (!decision.isCollaborative()) {
            return streamSingleAgent(decision.getPrimaryAgent(), enrichedMessage, chatId, toolContext);
        }

        log.info("Routing chat request: chatId={}, mode=collaborative, primaryAgent={}, executionOrder={}",
                chatId, decision.getPrimaryAgent(), decision.getExecutionOrder());

        return collaborativeCall(decision, enrichedMessage, chatId, toolContext)
                .flatMapMany(Flux::just)
                .onErrorResume(ex -> fallbackToGeneral(decision.getPrimaryAgent(), enrichedMessage, chatId, toolContext, ex));
    }

    /**
     * 返回字符串数据流。
     */
    private Flux<String> streamSingleAgent(AgentType agentType, String userMessage, String chatId, Map<String, Object> toolContext) {
        ChatClient selectedClient = selectChatClient(agentType);
        log.info("Routing chat request: chatId={}, mode=single, agentType={}", chatId, agentType);

        return streamWithRetryInternal(selectedClient, userMessage, chatId, toolContext)
                .onErrorResume(primaryEx -> fallbackToGeneral(agentType, userMessage, chatId, toolContext, primaryEx));
    }

    /**
     * 返回字符串数据流。
     */
    private Mono<String> collaborativeCall(AgentRoutingDecision decision, String userMessage, String chatId, Map<String, Object> toolContext) {
        return Flux.fromIterable(decision.getExecutionOrder())
                .concatMap(agentType -> callSpecialist(agentType, userMessage, chatId, toolContext)
                        .map(content -> new AgentAnswer(agentType, content))
                        .onErrorResume(ex -> {
                            log.warn("Collaborative specialist failed: agentType={}, chatId={}", agentType, chatId, ex);
                            return Mono.empty();
                        }))
                .collectList()
                .flatMap(answers -> synthesizeCollaborativeAnswer(decision, userMessage, chatId, toolContext, answers));
    }

    /**
     * 返回字符串数据流。
     */
    private Mono<String> callSpecialist(AgentType agentType, String userMessage, String chatId, Map<String, Object> toolContext) {
        ChatClient chatClient = selectChatClient(agentType);
        String prompt = """
                Multi-agent collaboration task.
                Current specialist role: %s
                Focus only on your domain and provide a concise, actionable conclusion.
                User message:
                %s
                """.formatted(agentType.name(), userMessage);

        return callAsMono(chatClient, prompt, chatId, toolContext);
    }

    /**
     * 返回字符串数据流。
     */
    private Mono<String> synthesizeCollaborativeAnswer(
            AgentRoutingDecision decision,
            String userMessage,
            String chatId,
            Map<String, Object> toolContext,
            List<AgentAnswer> answers
    ) {
        if (answers.isEmpty()) {
            return Mono.error(new IllegalStateException("No specialist output generated"));
        }

        if (answers.size() == 1) {
            return Mono.just(answers.get(0).content);
        }

        String synthesisPrompt = buildSynthesisPrompt(decision, userMessage, answers);
        return callAsMono(generalChatClient, synthesisPrompt, chatId, toolContext);
    }

    /**
     * 构建综合提示词。
     */
    private String buildSynthesisPrompt(AgentRoutingDecision decision, String userMessage, List<AgentAnswer> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the coordinator agent in a multi-agent workflow.\n");
        sb.append("Combine the specialist outputs into the final user-facing answer.\n");
        sb.append("Requirements:\n");
        sb.append("1) Keep the answer concise and practical.\n");
        sb.append("2) Prioritize concrete facts and tool results.\n");
        sb.append("3) If specialists disagree, choose one conclusion and explain why briefly.\n");
        sb.append("4) If key information is missing, state what is missing clearly.\n\n");
        sb.append("Primary specialist: ").append(decision.getPrimaryAgent()).append("\n");
        sb.append("User message:\n").append(userMessage).append("\n\n");
        sb.append("Specialist outputs:\n");

        for (AgentAnswer answer : answers) {
            sb.append("[").append(answer.agentType.name()).append("]\n")
                    .append(answer.content)
                    .append("\n\n");
        }

        sb.append("Output the final answer for the user directly.");
        return sb.toString();
    }

    /**
     * 返回字符串数据流。
     */
    private Mono<String> callAsMono(ChatClient chatClient, String prompt, String chatId, Map<String, Object> toolContext) {
        return streamWithRetryInternal(chatClient, prompt, chatId, toolContext)
                .collectList()
                .map(this::joinChunks)
                .flatMap(content -> {
                    if (content == null || content.isBlank()) {
                        return Mono.error(new IllegalStateException("Model returned empty content"));
                    }
                    return Mono.just(content);
                });
    }

    /**
     * 获取字符串结果。
     */
    private String joinChunks(List<String> chunks) {
        StringBuilder sb = new StringBuilder();
        for (String chunk : chunks) {
            if (chunk != null) {
                sb.append(chunk);
            }
        }
        return sb.toString().trim();
    }

    /**
     * 返回字符串数据流。
     */
    private Flux<String> fallbackToGeneral(AgentType agentType, String userMessage, String chatId, Map<String, Object> toolContext, Throwable primaryEx) {
        if (agentType == AgentType.GENERAL) {
            log.error("General agent failed: chatId={}", chatId, primaryEx);
            return Flux.just(FALLBACK_MESSAGE);
        }

        log.warn("Agent {} failed, falling back to general agent: chatId={}", agentType, chatId, primaryEx);
        return streamWithRetryInternal(generalChatClient, userMessage, chatId, toolContext)
                .onErrorResume(generalEx -> {
                    log.error("General fallback failed: chatId={}", chatId, generalEx);
                    return Flux.just(FALLBACK_MESSAGE);
                });
    }

    /**
     * 返回字符串数据流。
     */
    private Flux<String> streamWithRetryInternal(ChatClient chatClient, String userMessage, String chatId, Map<String, Object> toolContext) {
        return streamCallWithEmptyDetection(chatClient, userMessage, chatId, toolContext)
                .onErrorResume(firstEx -> {
                    log.warn("Stream call failed or returned empty content, retrying once: chatId={}", chatId, firstEx);
                    return streamCallWithEmptyDetection(chatClient, userMessage, chatId, toolContext)
                            .onErrorResume(secondEx -> nonStreamCall(chatClient, userMessage, chatId, toolContext, secondEx));
                });
    }

    /**
     * 返回字符串数据流。
     */
    private Flux<String> streamCallWithEmptyDetection(ChatClient chatClient, String userMessage, String chatId, Map<String, Object> toolContext) {
        AtomicBoolean hasContent = new AtomicBoolean(false);

        return streamCall(chatClient, userMessage, chatId, toolContext)
                .doOnNext(chunk -> {
                    if (chunk != null && !chunk.isBlank()) {
                        hasContent.set(true);
                    }
                })
                .concatWith(Mono.defer(() -> hasContent.get()
                        ? Mono.empty()
                        : Mono.error(new IllegalStateException("Stream call completed with empty content"))));
    }

    /**
     * 返回字符串数据流。
     */
    private Flux<String> streamCall(ChatClient chatClient, String userMessage, String chatId, Map<String, Object> toolContext) {
        return Flux.defer(() -> chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .toolContext(toolContext)
                .stream()
                .content());
    }

    /**
     * 返回字符串数据流。
     */
    private Flux<String> nonStreamCall(ChatClient chatClient, String userMessage, String chatId, Map<String, Object> toolContext, Throwable streamEx) {
        log.error("Stream call failed, falling back to non-stream call: chatId={}", chatId, streamEx);

        return Mono.fromCallable(() -> chatClient.prompt()
                        .user(userMessage)
                        .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                        .toolContext(toolContext)
                        .call()
                        .content())
                .flatMapMany(content -> {
                    if (content == null || content.isBlank()) {
                        return Mono.error(new IllegalStateException("Non-stream call returned empty content"));
                    }
                    return Flux.just(content);
                });
    }

    /**
     * 查询聊天客户端。
     */
    private ChatClient selectChatClient(AgentType agentType) {
        return switch (agentType) {
            case SHOP -> shopAgentChatClient;
            case PRODUCT -> productAgentChatClient;
            case REVIEW -> reviewAgentChatClient;
            case GENERAL -> generalChatClient;
        };
    }

    /**
     * 构建工具上下文。
     */
    private Map<String, Object> buildToolContext(AIChatRequest request, String chatId) {
        Map<String, Object> toolContext = new HashMap<>();
        if (request != null && request.getContext() != null) {
            toolContext.putAll(request.getContext());
        }
        toolContext.putIfAbsent("conversationId", chatId);
        return toolContext;
    }

    /**
     * 智能体回答类。
     */
    private static class AgentAnswer {
        private final AgentType agentType;
        private final String content;

        /**
         * 构造智能体回答。
         */
        private AgentAnswer(AgentType agentType, String content) {
            this.agentType = agentType;
            this.content = content;
        }
    }
}
