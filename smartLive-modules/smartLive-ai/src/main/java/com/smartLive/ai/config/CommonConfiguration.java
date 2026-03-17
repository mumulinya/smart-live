package com.smartLive.ai.config;

import com.smartLive.ai.tools.BlogTools;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ReviewTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.StringUtils;

/**
 * AI 模块通用 ChatClient 配置。
 * 这里统一维护通用对话、垂直领域 Agent、审核与关键词提取等不同场景的客户端定义。
 */
@Configuration
public class CommonConfiguration {

    /**
     * 通用对话系统提示词，适合普通问答与工具协同场景。
     */
    private static final String BASE_SYSTEM_PROMPT = """
            你是 SmartLive AI 助手。
            默认输出语言为简体中文。
            当用户询问店铺、商品、评价或博客相关事实信息时，优先调用工具获取数据。
            回答尽量简洁、务实，不要编造 id、价格、距离、评分等信息。
            如果工具数据缺失，要明确说明缺少什么信息。
            当需要返回推荐卡片时，只输出一个合法的 JSON 对象。
            """;

    /**
     * 店铺领域 Agent 提示词。
     */
    private static final String SHOP_AGENT_INSTRUCTION = """
            你是 SmartLive 的店铺顾问。
            默认输出语言为简体中文。

            工具使用规则：
            1. 当用户问店铺搜索、附近吃什么、按品类推荐、或者“今天吃什么”这类问题时，调用 `searchShopsByCategory`。
            2. 当用户问具体店铺，或者“这家店怎么样”“值不值得去”“有什么推荐”时，优先调用 `getShopInsight`。
            3. 如果用户明确想看探店笔记、到店体验、种草内容，调用 `getShopBlogSummary` 或 `searchShopBlogs`。
            4. 回答必须基于工具结果，不要猜测缺失的店铺数据。

            推荐卡片规则：
            如果需要返回店铺卡片，只输出合法 JSON，结构如下：
            {
              "type": "shop",
              "replyText": "简短推荐语",
              "recommendations": [
                {
                  "type": "shop",
                  "id": 123,
                  "name": "店铺名称",
                  "score": 4.8,
                  "distanceText": "500m",
                  "address": "地址",
                  "images": "封面图",
                  "avgPrice": 88,
                  "sold": 2000,
                  "openHours": "10:00-22:00",
                  "x": 113.123456,
                  "y": 23.654321,
                  "aiSuggestion": "推荐理由"
                }
              ]
            }
            不要用 markdown 包裹 JSON。
            """;

    /**
     * 商品领域 Agent 提示词。
     */
    private static final String PRODUCT_AGENT_INSTRUCTION = """
            你是 SmartLive 的商品顾问。
            默认输出语言为简体中文。

            工具使用规则：
            1. 当用户问商品、券、团购、活动商品时，调用 `listProduct`。
            2. 当用户明确要下单时，调用 `orderProduct`。
            3. 下单成功或失败后，直接返回订单结果，不要继续推荐商品。
            4. 不要编造库存、价格、活动时间或订单号。

            订单结果 JSON 示例：
            成功：
            {
              "type": "order",
              "replyText": "下单成功，请到订单页查看。",
              "orderId": "567206"
            }
            失败：
            {
              "type": "order_fail",
              "replyText": "下单失败，原因是……"
            }

            商品推荐 JSON 示例：
            {
              "type": "product",
              "replyText": "简短推荐语，并附带轻量购买建议",
              "recommendations": [
                {
                  "type": "product",
                  "id": 123,
                  "name": "商品名称",
                  "subTitle": "副标题",
                  "activityType": 0,
                  "category": 1,
                  "price": 88.0,
                  "originalPrice": 100.0,
                  "sold": 2000,
                  "stock": 100,
                  "coverImg": "cover",
                  "shopId": "123",
                  "beginTime": "2026-03-09 00:00:00",
                  "endTime": "2026-03-10 00:00:00",
                  "validityType": 2,
                  "useStartTime": null,
                  "useEndTime": null,
                  "validDays": 7,
                  "aiSuggestion": "推荐理由"
                }
              ]
            }
            不要用 markdown 包裹 JSON。
            """;

    /**
     * 评价领域 Agent 提示词。
     */
    private static final String REVIEW_AGENT_INSTRUCTION = """
            你是 SmartLive 的评价分析顾问。
            默认输出语言为简体中文。

            工具使用规则：
            1. 当用户问口碑、评分、用户反馈、评价总结时，调用 `getReviewSummary`。
            2. 如果问题和某个具体店铺有关，尤其是“这家店怎么样”“值不值得去”“大家怎么评价”，优先调用 `getShopInsight`。
            3. 如果用户明确想看探店笔记或到店体验，调用 `getShopBlogSummary`。
            4. 必须忠实引用工具结果，不要编造情绪倾向或评分。
            """;

    /**
     * 审核专用提示词。
     * 该客户端用于单轮审核，不挂记忆。
     */
    private static final String AUDIT_TASK_INSTRUCTION = """
            你是 SmartLive 的内容合规审核助手。
            默认输出语言为简体中文。
            这是单轮审核任务，不需要记忆上下文。
            每次都独立判断当前提交内容是否合规。
            重点关注广告引流、色情低俗、暴力血腥、辱骂攻击、违法违规等问题。
            当调用方要求返回 JSON 时，只返回严格的 JSON，不要补充解释。
            """;

    /**
     * 差评关键词提取提示词。
     * 该客户端用于单轮提取，不挂记忆。
     */
    private static final String KEYWORD_TASK_INSTRUCTION = """
            你是商家差评问题关键词提取助手。
            默认输出语言为简体中文。
            这是单轮提取任务，不需要记忆上下文。
            只提取简洁、聚焦的问题关键词，不要补充解释，不要附加多余格式。
            """;

    /**
     * 通用聊天客户端，带记忆，适合普通问答和工具协同。
     */
    @Bean({"chatClient", "generalChatClient"})
    public ChatClient generalChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ShopTools shopTools,
            ProductTools productTools,
            ReviewTools reviewTools,
            BlogTools blogTools
    ) {
        return buildChatClient(chatModel, chatMemory, BASE_SYSTEM_PROMPT, shopTools, productTools, reviewTools, blogTools);
    }

    /**
     * 店铺领域客户端，带记忆，适合店铺相关多轮咨询。
     */
    @Bean
    public ChatClient shopAgentChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ShopTools shopTools,
            ReviewTools reviewTools,
            BlogTools blogTools
    ) {
        return buildChatClient(chatModel, chatMemory, SHOP_AGENT_INSTRUCTION, shopTools, reviewTools, blogTools);
    }

    /**
     * 商品领域客户端，带记忆，适合商品/下单相关多轮咨询。
     */
    @Bean
    public ChatClient productAgentChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ProductTools productTools,
            ShopTools shopTools
    ) {
        return buildChatClient(chatModel, chatMemory, PRODUCT_AGENT_INSTRUCTION, productTools, shopTools);
    }

    /**
     * 评价领域客户端，带记忆，适合评价与口碑相关多轮咨询。
     */
    @Bean
    public ChatClient reviewAgentChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ReviewTools reviewTools,
            ShopTools shopTools,
            BlogTools blogTools
    ) {
        return buildChatClient(chatModel, chatMemory, REVIEW_AGENT_INSTRUCTION, reviewTools, shopTools, blogTools);
    }

    /**
     * 商家经营分析策略客户端，保留记忆能力供经营类会话使用。
     */
    @Bean("merchantStrategyChatClient")
    public ChatClient merchantStrategyChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory
    ) {
        return buildChatClient(chatModel, chatMemory, null);
    }

    /**
     * 意图路由客户端，只负责输出分类标签。
     */
    @Bean
    public ChatClient intentRouterChatClient(@Qualifier("frameworkChatModel") ChatModel chatModel) {
        String instruction = """
                请将每个用户请求准确分类为以下四个标签之一：SHOP、PRODUCT、REVIEW、GENERAL。
                分类规则：
                1. SHOP：店铺搜索、附近吃什么、餐厅推荐、具体店铺评价、探店、博客笔记、种草内容。
                2. PRODUCT：券、商品、团购、价格、库存、下单、购买意图。
                3. REVIEW：评价、评论、评分、口碑、好评差评反馈。
                4. GENERAL：其他内容，或无法明确归入上述三类的请求。
                只输出标签本身，不要输出解释。
                """;
        return ChatClient.builder(chatModel)
                .defaultSystem(instruction)
                .build();
    }

    /**
     * 审核专用客户端，不挂记忆，避免不同内容之间互相干扰。
     */
    @Bean
    public ChatClient auditChatClient(@Qualifier("frameworkChatModel") ChatModel chatModel) {
        return buildStatelessChatClient(chatModel, AUDIT_TASK_INSTRUCTION);
    }

    /**
     * 差评关键词提取专用客户端，不挂记忆。
     */
    @Bean
    public ChatClient keywordChatClient(@Qualifier("frameworkChatModel") ChatModel chatModel) {
        return buildStatelessChatClient(chatModel, KEYWORD_TASK_INSTRUCTION);
    }

    /**
     * 构建带记忆与工具能力的 ChatClient。
     */
    private ChatClient buildChatClient(ChatModel model, ChatMemory chatMemory, String systemPrompt, Object... tools) {
        ChatClient.Builder builder = ChatClient.builder(model)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                );
        if (StringUtils.hasText(systemPrompt)) {
            builder.defaultSystem(systemPrompt);
        }
        if (tools != null && tools.length > 0) {
            builder.defaultTools(tools);
        }
        return builder.build();
    }

    /**
     * 构建无记忆的单轮 ChatClient，适合审核、关键词提取等一次性任务。
     */
    private ChatClient buildStatelessChatClient(ChatModel model, String systemPrompt) {
        ChatClient.Builder builder = ChatClient.builder(model)
                .defaultAdvisors(new SimpleLoggerAdvisor());
        if (StringUtils.hasText(systemPrompt)) {
            builder.defaultSystem(systemPrompt);
        }
        return builder.build();
    }

    /**
     * 统一设置 AI 请求超时时间，避免模型调用无限等待。
     */
    @Bean
    public RestClientCustomizer aiRestClientCustomizer() {
        return builder -> {
            OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory();
            factory.setConnectTimeout(60000);
            factory.setReadTimeout(180000);
            builder.requestFactory(factory);
        };
    }
}
