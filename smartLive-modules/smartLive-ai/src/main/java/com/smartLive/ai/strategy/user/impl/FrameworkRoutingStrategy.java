package com.smartLive.ai.strategy.user.impl;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.config.prompt.AgentPromptCatalog;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.user.support.StructuredToolCaptureRegistry;
import com.smartLive.ai.strategy.user.AgentChatStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 框架路由策略类。
 */
@Slf4j
@Service
public class FrameworkRoutingStrategy implements AgentChatStrategy {

    private static final String ROUTER_NAME = "smartlive_auto_router";
    private static final String GENERAL_AGENT_NAME = "general_agent";

    private final ChatModel frameworkChatModel;
    private final Agent shopAgent;
    private final Agent productAgent;
    private final Agent reviewAgent;
    private final Agent generalAgent;
    private final ChatMemory chatMemory;
    private final SpringAIJacksonStateSerializer stateSerializer;
    private final StructuredToolCaptureRegistry captureRegistry;

    private volatile LlmRoutingAgent routingAgent;

    /**
     * 构造框架路由策略。
     */
    public FrameworkRoutingStrategy(
            @Qualifier("frameworkChatModel") ChatModel frameworkChatModel,
            @Qualifier("shopAgent") Agent shopAgent,
            @Qualifier("productAgent") Agent productAgent,
            @Qualifier("reviewAgent") Agent reviewAgent,
            @Qualifier("generalAgent") Agent generalAgent,
            ChatMemory chatMemory,
            StructuredToolCaptureRegistry captureRegistry
    ) {
        this.frameworkChatModel = frameworkChatModel;
        this.shopAgent = shopAgent;
        this.productAgent = productAgent;
        this.reviewAgent = reviewAgent;
        this.generalAgent = generalAgent;
        this.chatMemory = chatMemory;
        this.stateSerializer = new SpringAIJacksonStateSerializer(OverAllState::new);
        this.captureRegistry = captureRegistry;
    }

    /**
     * 返回字符串数据流。
     */
    @Override
    public Flux<String> streamChat(AIChatRequest chatRequest) {
        LlmRoutingAgent agent = getOrCreateRoutingAgent();
        if (agent == null) {
            return Flux.error(new IllegalStateException("Failed to build RoutingAgent"));
        }

        String chatId = resolveChatId(chatRequest);
        String enrichedMessage = buildEnrichedMessage(chatRequest);
        String captureId = resolveCaptureId(chatRequest);

        // 从 ChatMemory 读取历史对话，拼接到当前消息前面。
        // 阿里框架的 LlmRoutingAgent 不支持 Spring AI 的 ChatClient advisors，
        // 所以必须手动将历史上下文注入到 userMessage 中。
        String contextPrefix = buildContextPrefix(chatId);
        String fullMessage = contextPrefix + enrichedMessage;

        return Flux.defer(() -> streamFrameworkMessages(agent, chatId, fullMessage, captureId))
                .switchIfEmpty(Flux.defer(() -> {
                    log.warn("Framework routing returned empty result, trigger fallback...");
                    return Flux.error(new IllegalStateException("Framework routing returned empty result"));
                }));
    }

    /**
     * 从 ChatMemory 中读取历史消息，构建对话上下文前缀。
     */
    private String buildContextPrefix(String conversationId) {
        List<Message> history;
        try {
            history = chatMemory.get(conversationId);
        } catch (Exception ex) {
            log.warn("[FrameworkRouting] 读取 ChatMemory 失败, conversationId={}", conversationId, ex);
            return "";
        }

        if (history == null || history.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[Conversation history]\n");
        for (Message msg : history) {
            String role = msg.getMessageType() != null ? msg.getMessageType().name() : "UNKNOWN";
            String text = msg.getText();
            if (text != null && !text.isBlank()) {
                sb.append(role).append(": ").append(text).append("\n");
            }
        }
        sb.append("[End of history]\n\n");
        log.info("[FrameworkRouting] 注入 {} 条历史消息到上下文", history.size());
        return sb.toString();
    }

    private Flux<String> streamFrameworkMessages(LlmRoutingAgent agent, String chatId, String userMessage, String captureId) {
        return Flux.defer(() -> {
            try {
                // 每次请求使用独立 UUID 作为 threadId。
                // 原因：阿里框架会在同一 threadId 的 state 中累积历史消息，
                // 子 Agent 执行后产生的 GraphResponse 对象会残留在 messages 列表中，
                // 导致第二次路由时 RoutingNode 强转为 Message 报 ClassCastException。
                // 对话记忆已在 MessageTableChatMemoryManager 层管理，不依赖框架 threadId。
                String requestId = java.util.UUID.randomUUID().toString();
                com.alibaba.cloud.ai.graph.RunnableConfig config = com.alibaba.cloud.ai.graph.RunnableConfig.builder()
                        .threadId(requestId)
                        .build();
                log.info("[FrameworkRouting] invoke 开始调用, chatId={}, requestId={}", chatId, requestId);
                if (captureId != null) {
                    captureRegistry.activateCapture(captureId);
                }

                // 直接使用 invoke() 获取原生 OverAllState。
                // 注意：不用 invokeAndGetOutput()，因为它返回 NodeOutput 对象，
                // 内部 state 被序列化为 JSON 字符串，extractTextsDeep() 无法遍历。
                java.util.Optional<?> result = agent.invoke(userMessage, config);
                log.info("[FrameworkRouting] invoke 返回, present={}", result.isPresent());

                if (result.isPresent()) {
                    Object stateObj = result.get();
                    log.info("[FrameworkRouting] result 类型={}", stateObj.getClass().getName());
                    String extracted = extractFinalJsonFromState(stateObj);
                    if (hasText(extracted)) {
                        log.info("[FrameworkRouting] 提取成功, 长度={}", extracted.length());
                        return Flux.just(extracted);
                    }
                    log.warn("[FrameworkRouting] invoke 返回了结果但提取文本为空, state截取={}",
                            truncate(String.valueOf(stateObj), 500));
                }

                log.error("[FrameworkRouting] invoke 无法提取到有效输出");
                return Flux.error(new IllegalStateException("Framework routing returned empty result"));
            } catch (Exception ex) {
                log.error("[FrameworkRouting] 执行异常", ex);
                return Flux.error(ex);
            } finally {
                captureRegistry.clearActiveCapture();
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * 获取结构化工具结果捕获 id。
     */
    private String resolveCaptureId(AIChatRequest chatRequest) {
        if (chatRequest == null || chatRequest.getContext() == null) {
            return null;
        }
        Object requestId = chatRequest.getContext().get(StructuredToolCaptureRegistry.REQUEST_ID_KEY);
        return requestId == null ? null : String.valueOf(requestId);
    }

    /**
     * 深度遍历图状态对象（Map、OverAllState、Message），提取最后一条或合法的 JSON 数据。
     */
    private String extractFinalJsonFromState(Object stateObj) {
        List<String> assistantTexts = new java.util.ArrayList<>();
        extractTextsDeep(stateObj, assistantTexts);

        log.info("[FrameworkRouting] extractFinalJsonFromState: 提取到 {} 条 Assistant 文本", assistantTexts.size());
        for (int i = 0; i < assistantTexts.size(); i++) {
            log.debug("[FrameworkRouting]   文本[{}]: {}", i, truncate(assistantTexts.get(i), 200));
        }

        // 尝试从所有提取到的 Assistant 文本中找寻完整的 JSON（优先使用最后一个）
        for (int i = assistantTexts.size() - 1; i >= 0; i--) {
            String text = assistantTexts.get(i);
            String json = cleanAndExtractJson(text);
            if (hasText(json)) {
                return json;
            }
        }

        // 如果 assistantTexts 不为空但没有 JSON，返回拼接文本
        if (!assistantTexts.isEmpty()) {
            return String.join("\n\n", assistantTexts);
        }

        // 最终降级：直接将 stateObj 转为字符串
        String fallback = String.valueOf(stateObj);
        log.warn("[FrameworkRouting] 未提取到任何 Assistant 文本, state toString 截取={}", truncate(fallback, 500));
        return null;
    }

    private void extractTextsDeep(Object obj, List<String> texts) {
        if (obj == null) return;

        // 1) Spring AI Message 对象
        if (obj instanceof org.springframework.ai.chat.messages.Message msg) {
            if (msg.getMessageType() == org.springframework.ai.chat.messages.MessageType.ASSISTANT && hasText(msg.getText())) {
                texts.add(msg.getText());
            }
            return;
        }

        // 2) ChatResponse / Generation / AssistantMessage
        if (obj instanceof org.springframework.ai.chat.model.ChatResponse chatResp) {
            if (chatResp.getResult() != null && chatResp.getResult().getOutput() != null) {
                String content = chatResp.getResult().getOutput().getText();
                if (hasText(content)) {
                    texts.add(content);
                }
            }
            return;
        }

        // 3) Collection 递归
        if (obj instanceof java.util.Collection<?> col) {
            for (Object item : col) {
                extractTextsDeep(item, texts);
            }
            return;
        }

        // 4) Map - 包含 Jackson 反序列化兼容
        if (obj instanceof java.util.Map<?, ?> map) {
            // 直接检查 messageType 为 ASSISTANT 的 map 项
            if ("ASSISTANT".equalsIgnoreCase(String.valueOf(map.get("messageType"))) && map.containsKey("text")) {
                Object textObj = map.get("text");
                if (textObj instanceof String s && hasText(s)) {
                    texts.add(s);
                }
            }
            // 检查 content 字段（某些框架版本用 content 代替 text）
            if ("ASSISTANT".equalsIgnoreCase(String.valueOf(map.get("messageType"))) && map.containsKey("content")) {
                Object contentObj = map.get("content");
                if (contentObj instanceof String s && hasText(s)) {
                    texts.add(s);
                }
            }
            // 优先检查 "messages" key
            if (map.containsKey("messages")) {
                extractTextsDeep(map.get("messages"), texts);
            }
            // 优先检查 "output" key
            if (map.containsKey("output")) {
                Object outputObj = map.get("output");
                if (outputObj instanceof String s && hasText(s)) {
                    texts.add(s);
                } else {
                    extractTextsDeep(outputObj, texts);
                }
            }
            // 继续深层遍历所有 value
            for (Object value : map.values()) {
                if (value != null && !(value instanceof String)) {
                    extractTextsDeep(value, texts);
                }
            }
            return;
        }

        // 5) 字符串直接作为文本（兜底）
        if (obj instanceof String s && hasText(s)) {
            texts.add(s);
            return;
        }

        // 6) 反射尝试常用方法：data(), messages(), getMessages(), getOutput(), getText(), getContent()
        for (String methodName : new String[]{"data", "messages", "getMessages", "getOutput", "getText", "getContent"}) {
            try {
                Method m = obj.getClass().getMethod(methodName);
                Object result = m.invoke(obj);
                if (result != null && result != obj) {
                    extractTextsDeep(result, texts);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 从混杂了 Markdown 的字符串中提取最外层的原生 JSON。
     */
    private String cleanAndExtractJson(String text) {
        if (text == null) return "";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return text.substring(start, end + 1);
        }
        return "";
    }

    /**
     * 获取create路由智能体。
     */
    private LlmRoutingAgent getOrCreateRoutingAgent() {
        LlmRoutingAgent cached = this.routingAgent;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (this.routingAgent != null) {
                return this.routingAgent;
            }

            try {
                this.routingAgent = buildRoutingAgent();
                log.info("Spring AI Alibaba framework routing is enabled");
            } catch (Exception ex) {
                log.error("Failed to initialize framework routing, fallback will be used", ex);
                this.routingAgent = null;
            }

            return this.routingAgent;
        }
    }

    /**
     * 构建路由智能体。
     */
    private LlmRoutingAgent buildRoutingAgent() {
        List<Agent> subAgents = List.of(shopAgent, productAgent, reviewAgent, generalAgent);

        return LlmRoutingAgent.builder()
                .name(ROUTER_NAME)
                .description("SmartLive auto routing agent")
                .model(frameworkChatModel)
                .stateSerializer(stateSerializer)
                .subAgents(subAgents)
                .fallbackAgent(GENERAL_AGENT_NAME)
                .systemPrompt(AgentPromptCatalog.FRAMEWORK_ROUTER_SYSTEM_PROMPT)
                .instruction(AgentPromptCatalog.FRAMEWORK_ROUTER_INSTRUCTION)
                .build();
    }

    /**
     * 判断文本是否存在。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 截取字符串到指定最大长度。
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "null";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
