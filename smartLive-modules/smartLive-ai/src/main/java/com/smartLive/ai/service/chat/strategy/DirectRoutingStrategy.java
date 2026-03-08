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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 多 Agent 聊天协作服务（原生 Spring AI 实现）。
 * <p>
 * 这是项目中的【第二套 Agent 方案】：
 * 1. 架构：基于 Spring AI 原生 ChatClient 和手写的路由决策（AgentRouter）实现。
 * 2. 职责：提供透明的、基于规则和关键词的意图分类与专家协作逻辑。
 * 3. 地位：作为系统的【降级 / 回退（Fallback）方案】。当 Spring AI Alibaba 框架不可用或处理失败时，
 *    系统会切换到此服务，以确保基础对话和专家协作能力的可用性。
 */
@Slf4j
@Service
public class DirectRoutingStrategy implements AgentChatStrategy {

    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    private static final String FALLBACK_MESSAGE = "抱歉，当前服务繁忙，请稍后重试。";

    private final AgentRouter agentRouter;
    private final ChatClient generalChatClient;
    private final ChatClient shopAgentChatClient;
    private final ChatClient productAgentChatClient;
    private final ChatClient reviewAgentChatClient;

    /**
     * 构造多 Agent 聊天服务。
     */
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
     * 对外聊天入口。
     * 根据路由决策执行单 Agent 或多 Agent 协作，并在失败时进行降级处理。
     */
    @Override
    public Flux<String> streamChat(AIChatRequest request, String chatId) {
        AgentRoutingDecision decision = agentRouter.routeDecision(request.getMessage());
        String enrichedMessage = buildEnrichedMessage(request);

        if (!decision.isCollaborative()) {
            return streamSingleAgent(decision.getPrimaryAgent(), enrichedMessage, chatId);
        }

        log.info("路由聊天请求: chatId={}, 模式=协作, 主Agent={}, 执行顺序={}，用户组装消息为：{}",
                chatId, decision.getPrimaryAgent(), decision.getExecutionOrder(), enrichedMessage);

        return collaborativeCall(decision, enrichedMessage, chatId)
                .flatMapMany(Flux::just)
                .onErrorResume(ex -> fallbackToGeneral(decision.getPrimaryAgent(), enrichedMessage, chatId, ex));
    }

    /**
     * 执行单 Agent 模式。
     */
    private Flux<String> streamSingleAgent(AgentType agentType, String userMessage, String chatId) {
        ChatClient selectedClient = selectChatClient(agentType);
        log.info("路由聊天请求: chatId={}, 模式=单Agent, agentType={},用户组装消息为：{}\"", chatId, agentType, userMessage);

        return streamWithRetryInternal(selectedClient, userMessage, chatId)
                .onErrorResume(primaryEx -> fallbackToGeneral(agentType, userMessage, chatId, primaryEx));
    }

    /**
     * 执行协作模式：按顺序调用多个专家 Agent，并汇总结果。
     */
    private Mono<String> collaborativeCall(AgentRoutingDecision decision, String userMessage, String chatId) {
        return Flux.fromIterable(decision.getExecutionOrder())
                .concatMap(agentType -> callSpecialist(agentType, userMessage, buildAgentChatId(chatId, agentType))
                        .map(content -> new AgentAnswer(agentType, content))
                        .onErrorResume(ex -> {
                            log.warn("协作模式下专家Agent执行失败: agentType={}, chatId={}", agentType, chatId, ex);
                            return Mono.empty();
                        }))
                .collectList()
                .flatMap(answers -> synthesizeCollaborativeAnswer(decision, userMessage, chatId, answers));
    }

    /**
     * 调用单个专家 Agent。
     */
    private Mono<String> callSpecialist(AgentType agentType, String userMessage, String chatId) {
        ChatClient chatClient = selectChatClient(agentType);
        String prompt = """
                多智能体协作任务。
                当前专家角色：%s
                请只关注你的专业领域，给出简洁且可执行的结论。
                用户消息：
                %s
                """.formatted(agentType.name(), userMessage);

        return callAsMono(chatClient, prompt, chatId);
    }

    /**
     * 汇总协作结果：单条结果直接返回，多条结果交给通用 Agent 进行整合。
     */
    private Mono<String> synthesizeCollaborativeAnswer(
            AgentRoutingDecision decision,
            String userMessage,
            String chatId,
            List<AgentAnswer> answers
    ) {
        if (answers.isEmpty()) {
            return Mono.error(new IllegalStateException("没有生成任何专家输出"));
        }

        if (answers.size() == 1) {
            return Mono.just(answers.get(0).content);
        }

        String synthesisPrompt = buildSynthesisPrompt(decision, userMessage, answers);
        return callAsMono(generalChatClient, synthesisPrompt, chatId);
    }

    /**
     * 构造通用 Agent 的汇总提示词。
     */
    private String buildSynthesisPrompt(AgentRoutingDecision decision, String userMessage, List<AgentAnswer> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是多智能体协作中的总协调Agent。\\n");
        sb.append("请将各专家输出整合为最终给用户的回复。\\n");
        sb.append("要求：\\n");
        sb.append("1) 回复简洁、务实。\\n");
        sb.append("2) 优先保留明确事实和工具结果。\\n");
        sb.append("3) 若专家结论冲突，简要说明采用哪一个结论及原因。\\n");
        sb.append("4) 若关键信息缺失，明确指出缺失项。\\n\\n");
        sb.append("主专家：").append(decision.getPrimaryAgent()).append("\\n");
        sb.append("用户消息：\\n").append(userMessage).append("\\n\\n");
        sb.append("专家输出：\\n");

        for (AgentAnswer answer : answers) {
            sb.append("[").append(answer.agentType.name()).append("]\\n")
                    .append(answer.content)
                    .append("\\n\\n");
        }

        sb.append("请直接输出最终给用户的回复。");
        return sb.toString();
    }

    /**
     * 统一调用入口：将流式/非流式输出合并为单个字符串。
     */
    private Mono<String> callAsMono(ChatClient chatClient, String prompt, String chatId) {
        return streamWithRetryInternal(chatClient, prompt, chatId)
                .collectList()
                .map(this::joinChunks)
                .flatMap(content -> {
                    if (content == null || content.isBlank()) {
                        return Mono.error(new IllegalStateException("模型返回内容为空"));
                    }
                    return Mono.just(content);
                });
    }

    /**
     * 合并流式分片内容。
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
     * 构造专家 Agent 会话 ID，避免污染主会话记忆。
     */
    private String buildAgentChatId(String chatId, AgentType agentType) {
        return chatId + "::" + agentType.name().toLowerCase(Locale.ROOT);
    }

    /**
     * 专家 Agent 失败时降级到通用 Agent。
     */
    private Flux<String> fallbackToGeneral(AgentType agentType, String userMessage, String chatId, Throwable primaryEx) {
        if (agentType == AgentType.GENERAL) {
            log.error("通用Agent执行失败: chatId={}", chatId, primaryEx);
            return Flux.just(FALLBACK_MESSAGE);
        }

        log.warn("Agent {} 执行失败，降级到通用Agent: chatId={}", agentType, chatId, primaryEx);
        return streamWithRetryInternal(generalChatClient, userMessage, chatId)
                .onErrorResume(generalEx -> {
                    log.error("通用Agent降级后仍失败: chatId={}", chatId, generalEx);
                    return Flux.just(FALLBACK_MESSAGE);
                });
    }

    /**
     * 统一重试策略：流式失败后重试一次，再降级到非流式调用。
     */
    private Flux<String> streamWithRetryInternal(ChatClient chatClient, String userMessage, String chatId) {
        return streamCallWithEmptyDetection(chatClient, userMessage, chatId)
                .onErrorResume(firstEx -> {
                    log.warn("流式调用失败或返回空内容，执行一次重试: chatId={}", chatId, firstEx);
                    return streamCallWithEmptyDetection(chatClient, userMessage, chatId)
                            .onErrorResume(secondEx -> nonStreamCall(chatClient, userMessage, chatId, secondEx));
                });
    }

    /**
     * 流式调用并检测“正常结束但内容为空”的场景。
     */
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
                        : Mono.error(new IllegalStateException("流式调用结束但内容为空"))));
    }

    /**
     * 原生流式调用。
     */
    private Flux<String> streamCall(ChatClient chatClient, String userMessage, String chatId) {
        return Flux.defer(() -> chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content());
    }

    /**
     * 流式失败后的非流式降级调用。
     */
    private Flux<String> nonStreamCall(ChatClient chatClient, String userMessage, String chatId, Throwable streamEx) {
        log.error("流式调用失败，降级为非流式调用: chatId={}", chatId, streamEx);

        return Mono.fromCallable(() -> chatClient.prompt()
                        .user(userMessage)
                        .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                        .call()
                        .content())
                .flatMapMany(content -> {
                    if (content == null || content.isBlank()) {
                        return Mono.error(new IllegalStateException("非流式调用返回内容为空"));
                    }
                    return Flux.just(content);
                });
    }

    /**
     * 根据 Agent 类型选择对应的 ChatClient。
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
     * 专家输出对象。
     */
    private static class AgentAnswer {
        private final AgentType agentType;
        private final String content;

        /**
         * 构造专家输出对象。
         */
        private AgentAnswer(AgentType agentType, String content) {
            this.agentType = agentType;
            this.content = content;
        }
    }
}
