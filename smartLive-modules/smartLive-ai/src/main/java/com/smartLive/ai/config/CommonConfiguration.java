package com.smartLive.ai.config;

import com.smartLive.ai.config.prompt.AgentPromptCatalog;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI 模块通用 ChatClient 配置。
 * 这里统一维护通用对话、垂直领域 Agent、审核与关键词提取等不同场景的客户端定义。
 */
@Configuration
public class CommonConfiguration {

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
        return buildChatClient(chatModel, chatMemory, AgentPromptCatalog.GENERAL_AGENT_INSTRUCTION, shopTools, productTools, reviewTools, blogTools);
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
        return buildChatClient(chatModel, chatMemory, AgentPromptCatalog.SHOP_AGENT_INSTRUCTION, shopTools, reviewTools, blogTools);
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
        return buildChatClient(chatModel, chatMemory, AgentPromptCatalog.PRODUCT_AGENT_INSTRUCTION, productTools, shopTools);
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
        return buildChatClient(chatModel, chatMemory, AgentPromptCatalog.REVIEW_AGENT_INSTRUCTION, reviewTools, shopTools, blogTools);
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
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem(AgentPromptCatalog.KEYWORD_EXTRACTOR_SYSTEM_PROMPT)
                .build();
    }

    /**
     * 差评关键词提取专用客户端，不挂记忆。
     */
    @Bean
    public ChatClient keywordChatClient(@Qualifier("frameworkChatModel") ChatModel chatModel) {
        String keywordPrompt = AgentPromptCatalog.KEYWORD_EXTRACTOR_SYSTEM_PROMPT;
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem(keywordPrompt)
                .build();
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
     * 统一设置 AI 请求超时时间，避免模型调用无限等待。
     */
    @Bean
    public RestClientCustomizer aiRestClientCustomizer() {
        return builder -> {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(Duration.ofSeconds(180));
            builder.requestFactory(factory);
        };
    }
}
