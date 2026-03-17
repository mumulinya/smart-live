package com.smartLive.ai.service.chat.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.chat.AgentChatStrategy;
import com.smartLive.ai.service.rag.IProductRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Spring AI 的自主反思（ReAct/Plan-and-Solve 风格）Agent 聊天服务。
 * 这是项目中的【第三套 Agent 方案】：
 * 1. 架构：纯基于单模型推理 + 本地大循环（While loop）实现复杂规划、工具调用、自我反思。
 * 2. 职责：处理极高复杂度、需要逐步推导的复合任务。
 * 3. 特点：牺牲了部分的并发执行效率（较慢），但换来了更高的逻辑严密性和问题解决成功率。
 */
@Slf4j
@Service
public class AutonomousAgentStrategy implements AgentChatStrategy {

    private static final int MAX_ITERATIONS = 5;
    private static final String FALLBACK_MESSAGE = "抱歉，自主任务执行失败。";

    private static final String SYSTEM_PROMPT = """
            你是一个具备自主规划架构和工具调用能力的超级智能助手。
            对于每次用户请求，你必须且只能输出合法的 JSON 格式的决策。
            禁止输出任何非 JSON 格式的说明文字。

            可用的工具集（以列表形式说明）：
            - TOOL_SEARCH_PRODUCT：按关键词搜索或推荐商品信息。
            - TOOL_GET_SHOP_METRICS：获取店铺的统计评价信息（基于店铺 ID）。
            - TOOL_ANALYZE_REVIEWS：提取并分析商品的真实用户买家评价评价。
            - TOOL_AIGC_GENERATE：生成文案、生成推荐理由或内容。

            每次你需要输出的 JSON 结构必须严格遵守以下格式，任何字段不可缺失：
            {
                "thought": "（描述你当前在思考什么，需要验证什么，或是下一步想干什么，必填）",
                "action": "（如果你需要使用工具，填入对应的工具名称；如果你认为已收集足够信息或只需要直接回复用户，请填入 'REPLY'，必填）",
                "action_input": "（如果你使用了某个工具，填入该工具需要的输入参数，例如关键词或ID等；如果 action 是 'REPLY'，这里可以为空，必填）",
                "final_answer": "（如果你认为已经完成任务，即 action 为 'REPLY'，请把生成最终给用户的完整、直接的答案写在这里；否则为空，必填）"
            }

            请开始你的推理。你的每一次回答都必须能被 JSON 对象反序列化解析。请确保输出只有JSON内容。
            """;

    private final ChatClient generalChatClient;
    private final IProductRagService productRagService;
    private final IShopRagService shopRagService;
    private final ObjectMapper objectMapper;

    /**
     * 构造自主智能体策略。
     */
    public AutonomousAgentStrategy(
            @Qualifier("generalChatClient") ChatClient generalChatClient,
            IProductRagService productRagService,
            IShopRagService shopRagService,
            ObjectMapper objectMapper
    ) {
        this.generalChatClient = generalChatClient;
        this.productRagService = productRagService;
        this.shopRagService = shopRagService;
        this.objectMapper = objectMapper;
    }

    /**
     * 返回字符串数据流。
     */
    @Override
    public Flux<String> streamChat(AIChatRequest chatRequest) {
        String chatId = resolveChatId(chatRequest);
        String enrichedMessage = buildEnrichedMessage(chatRequest);
        log.info("开始自主执行任务: chatId={}, userMessage={}", chatId, enrichedMessage);
        return executeAutonomousLoop(enrichedMessage, chatId)
                .onErrorResume(ex -> {
                    log.error("自主反思循环执行失败: chatId={}", chatId, ex);
                    return Flux.just(FALLBACK_MESSAGE);
                });
    }

    /**
     * 自主循环核心逻辑
     * 1. 初始化对话历史，注入 System Prompt。
     * 2. 进入 ReAct (Thought -> Action -> Observation) 循环。
     * 3. 解析模型返回的 JSON，提取思考、行动和最终答案。
     * 4. 如果是 REPLY，则结束循环并广播最终答案。
     * 5. 如果是工具调用，则执行对应工具并将结果反馈给模型，继续下一轮迭代。
     *
     * @param userMessage 用户原始输入
     * @param chatId 会话 ID
     * @return 最终响应的 Flux 流
     */
    private Flux<String> executeAutonomousLoop(String userMessage, String chatId) {
        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage(SYSTEM_PROMPT));
        history.add(new UserMessage(userMessage));

        return Flux.create(sink -> {
            boolean done = false;
            int iteration = 0;

            while (!done && iteration < MAX_ITERATIONS) {
                iteration++;
                log.info("自主循环 {}/{} | userMessage={}", iteration, MAX_ITERATIONS, userMessage);

                Prompt prompt = new Prompt(history);
                String modelResponse;
                try {
                    modelResponse = generalChatClient.prompt(prompt).call().content();
                    history.add(new AssistantMessage(modelResponse));
                } catch (Exception ex) {
                    sink.error(ex);
                    return;
                }

                Map<String, String> decision;
                try {
                    String cleanResponse = extractJson(modelResponse);
                    decision = objectMapper.readValue(cleanResponse, new TypeReference<Map<String, String>>() {});
                } catch (Exception ex) {
                    log.warn("大模型输出解析异常: {}", modelResponse, ex);
                    history.add(new UserMessage("JSON格式错误或内容无效，请严格按要求返回合法的 JSON！"));
                    continue;
                }

                String action = decision.get("action");
                String actionInput = decision.get("action_input");
                String finalAnswer = decision.get("final_answer");

                if ("REPLY".equalsIgnoreCase(action) && finalAnswer != null && !finalAnswer.isBlank()) {
                    log.info("返回最终结论: {}", finalAnswer);
                    sink.next(finalAnswer);
                    sink.complete();
                    done = true;
                } else {
                    log.info("执行动作: {} | 参数: {}", action, actionInput);
                    String toolResult = executeTool(action, actionInput);
                    log.info("动作执行结果: {}", toolResult);
                    history.add(new UserMessage("【工具执行结果】：" + toolResult + "\\n请基于此结果继续你的推理。"));
                }
            }

            if (!done) {
                log.warn("达到最大循环次数({}), 任务强制结束", MAX_ITERATIONS);
                sink.error(new IllegalStateException("达到最大循环次数"));
            }
        });
    }

    /**
     * 从模型返回的混合文本中提取标准的 JSON 字符串
     */
    private String extractJson(String text) {
        if (text == null) return "{}";
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && start < end) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * 工具分发与执行逻辑
     * 根据模型决策中的 action 名称，调度具体的 RAG 服务或工具方法
     *
     * @param action 工具名称（如 TOOL_SEARCH_PRODUCT）
     * @param input 工具输入参数
     * @return 工具执行后的文本结果
     */
    private String executeTool(String action, String input) {
        try {
            switch (action) {
                case "TOOL_SEARCH_PRODUCT":
                    java.util.List<com.smartLive.ai.entity.vo.ProductVO> products = productRagService.getProductList(new com.smartLive.ai.entity.vo.ProductVO(), input);
                    return objectMapper.writeValueAsString(products);
                case "TOOL_GET_SHOP_METRICS":
                    java.util.List<com.smartLive.ai.entity.vo.ShopVO> shops = shopRagService.getShopList(new com.smartLive.ai.entity.vo.ShopVO(), input);
                    return objectMapper.writeValueAsString(shops);
                case "TOOL_AIGC_GENERATE":
                     return "这是基于AIGC生成的文案和内容：" + input;
                default:
                    return "未知的工具：" + action;
            }
        } catch (Exception e) {
            return "工具执行异常: " + e.getMessage();
        }
    }
}
