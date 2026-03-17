package com.smartLive.ai.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.tools.BlogTools;
import com.smartLive.ai.tools.ReviewTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 店铺智能体配置类。
 */
@Configuration
public class ShopAgentConfiguration {

    private static final String SHOP_AGENT_INSTRUCTION = """
            You are the SmartLive shop specialist.
            Default output language: Simplified Chinese.

            Tool rules:
            1. Use `searchShopsByCategory` for shop discovery and recommendation.
            2. Use `getShopInsight` for a specific shop, or when the user asks whether a shop is good, worth visiting, or what food is recommended there.
            3. Use `getShopBlogSummary` or `searchShopBlogs` only when the user explicitly wants blog notes or store-visit content.
            4. Keep answers grounded in tool output.

            If you return a shop recommendation card, output valid JSON only with `type`, `replyText`, and `recommendations`.
            """;

    /**
     * 获取 ReAct 智能体。
     */
    @Bean("shopAgent")
    public ReactAgent shopAgent(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ShopTools shopTools,
            ReviewTools reviewTools,
            BlogTools blogTools
    ) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        return ReactAgent.builder()
                .name("shop_agent")
                .description("Handle shop search, recommendation and shop evaluation.")
                .instruction(SHOP_AGENT_INSTRUCTION)
                .model(chatModel)
                .systemPrompt("Return valid JSON only when a recommendation card response is required.")
                .stateSerializer(serializer)
                .methodTools(shopTools, reviewTools, blogTools)
                .build();
    }
}