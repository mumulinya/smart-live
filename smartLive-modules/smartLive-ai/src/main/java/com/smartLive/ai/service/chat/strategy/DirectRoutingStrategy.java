package com.smartLive.ai.service.chat.strategy;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.chat.AgentChatStrategy;
import com.smartLive.ai.service.chat.support.AgentRouter;
import com.smartLive.ai.service.chat.support.AgentRoutingDecision;
import com.smartLive.ai.service.chat.support.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Override
    public Flux<String> streamChat(AIChatRequest request) {
        String chatId = resolveChatId(request);
        AgentRoutingDecision decision = agentRouter.routeDecision(request.getMessage());
        String enrichedMessage = buildEnrichedMessage(request);

        if (!decision.isCollaborative()) {
            return streamSingleAgent(decision.getPrimaryAgent(), enrichedMessage, chatId);
        }

        log.info("Routing chat request: chatId={}, mode=collaborative, primaryAgent={}, executionOrder={}",
                chatId, decision.getPrimaryAgent(), decision.getExecutionOrder());

        return collaborativeCall(decision, enrichedMessage, chatId)
                .flatMapMany(Flux::just)
                .onErrorResume(ex -> fallbackToGeneral(decision.getPrimaryAgent(), enrichedMessage, chatId, ex));
    }

    private Flux<String> streamSingleAgent(AgentType agentType, String userMessage, String chatId) {
        ChatClient selectedClient = selectChatClient(agentType);
        log.info("Routing chat request: chatId={}, mode=single, agentType={}", chatId, agentType);

        return streamWithRetryInternal(selectedClient, userMessage, chatId)
                .onErrorResume(primaryEx -> fallbackToGeneral(agentType, userMessage, chatId, primaryEx));
    }

    private Mono<String> collaborativeCall(AgentRoutingDecision decision, String userMessage, String chatId) {
        return Flux.fromIterable(decision.getExecutionOrder())
                .concatMap(agentType -> callSpecialist(agentType, userMessage, chatId)
                        .map(content -> new AgentAnswer(agentType, content))
                        .onErrorResume(ex -> {
                            log.warn("Collaborative specialist failed: agentType={}, chatId={}", agentType, chatId, ex);
                            return Mono.empty();
                        }))
                .collectList()
                .flatMap(answers -> synthesizeCollaborativeAnswer(decision, userMessage, chatId, answers));
    }

    private Mono<String> callSpecialist(AgentType agentType, String userMessage, String chatId) {
        ChatClient chatClient = selectChatClient(agentType);
        String prompt = """
                Multi-agent collaboration task.
                Current specialist role: %s
                Focus only on your domain and provide a concise, actionable conclusion.
                User message:
                %s
                """.formatted(agentType.name(), userMessage);

        return callAsMono(chatClient, prompt, chatId);
    }

    private Mono<String> synthesizeCollaborativeAnswer(
            AgentRoutingDecision decision,
            String userMessage,
            String chatId,
            List<AgentAnswer> answers
    ) {
        if (answers.isEmpty()) {
            return Mono.error(new IllegalStateException("No specialist output generated"));
        }

        if (answers.size() == 1) {
            return Mono.just(answers.get(0).content);
        }

        String synthesisPrompt = buildSynthesisPrompt(decision, userMessage, answers);
        return callAsMono(generalChatClient, synthesisPrompt, chatId);
    }

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

    private Mono<String> callAsMono(ChatClient chatClient, String prompt, String chatId) {
        return streamWithRetryInternal(chatClient, prompt, chatId)
                .collectList()
                .map(this::joinChunks)
                .flatMap(content -> {
                    if (content == null || content.isBlank()) {
                        return Mono.error(new IllegalStateException("Model returned empty content"));
                    }
                    return Mono.just(content);
                });
    }

    private String joinChunks(List<String> chunks) {
        StringBuilder sb = new StringBuilder();
        for (String chunk : chunks) {
            if (chunk != null) {
                sb.append(chunk);
            }
        }
        return sb.toString().trim();
    }

    private Flux<String> fallbackToGeneral(AgentType agentType, String userMessage, String chatId, Throwable primaryEx) {
        if (agentType == AgentType.GENERAL) {
            log.error("General agent failed: chatId={}", chatId, primaryEx);
            return Flux.just(FALLBACK_MESSAGE);
        }

        log.warn("Agent {} failed, falling back to general agent: chatId={}", agentType, chatId, primaryEx);
        return streamWithRetryInternal(generalChatClient, userMessage, chatId)
                .onErrorResume(generalEx -> {
                    log.error("General fallback failed: chatId={}", chatId, generalEx);
                    return Flux.just(FALLBACK_MESSAGE);
                });
    }

    private Flux<String> streamWithRetryInternal(ChatClient chatClient, String userMessage, String chatId) {
        return streamCallWithEmptyDetection(chatClient, userMessage, chatId)
                .onErrorResume(firstEx -> {
                    log.warn("Stream call failed or returned empty content, retrying once: chatId={}", chatId, firstEx);
                    return streamCallWithEmptyDetection(chatClient, userMessage, chatId)
                            .onErrorResume(secondEx -> nonStreamCall(chatClient, userMessage, chatId, secondEx));
                });
    }

    private Flux<String> streamCallWithEmptyDetection(ChatClient chatClient, String userMessage, String chatId) {
        AtomicBoolean hasContent = new AtomicBoolean(false);

        return streamCall(chatClient, userMessage, chatId)
                .doOnNext(chunk -> {
                    if (chunk != null && !chunk.isBlank()) {
                        hasContent.set(true);
                    }
                })
                .concatWith(Mono.defer(() -> hasContent.get()
                        ? Mono.empty()
                        : Mono.error(new IllegalStateException("Stream call completed with empty content"))));
    }

    private Flux<String> streamCall(ChatClient chatClient, String userMessage, String chatId) {
        return Flux.defer(() -> chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content());
    }

    private Flux<String> nonStreamCall(ChatClient chatClient, String userMessage, String chatId, Throwable streamEx) {
        log.error("Stream call failed, falling back to non-stream call: chatId={}", chatId, streamEx);

        return Mono.fromCallable(() -> chatClient.prompt()
                        .user(userMessage)
                        .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                        .call()
                        .content())
                .flatMapMany(content -> {
                    if (content == null || content.isBlank()) {
                        return Mono.error(new IllegalStateException("Non-stream call returned empty content"));
                    }
                    return Flux.just(content);
                });
    }

    private ChatClient selectChatClient(AgentType agentType) {
        return switch (agentType) {
            case SHOP -> shopAgentChatClient;
            case PRODUCT -> productAgentChatClient;
            case REVIEW -> reviewAgentChatClient;
            case GENERAL -> generalChatClient;
        };
    }

    private static class AgentAnswer {
        private final AgentType agentType;
        private final String content;

        private AgentAnswer(AgentType agentType, String content) {
            this.agentType = agentType;
            this.content = content;
        }
    }
}