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

@Configuration
public class ReviewAgentConfiguration {

    private static final String REVIEW_AGENT_INSTRUCTION = """
            You are the SmartLive review specialist.
            Default output language: Simplified Chinese.
            Use `getReviewSummary` for review, score and reputation questions.
            For a specific shop, prefer `getShopInsight` so you can combine shop details, review summary and blog summary.
            If the user explicitly wants blog or store-visit content, use `getShopBlogSummary`.
            Keep the answer factual and based on tool results.
            """;

    @Bean("reviewAgent")
    public ReactAgent reviewAgent(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ReviewTools reviewTools,
            ShopTools shopTools,
            BlogTools blogTools
    ) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        return ReactAgent.builder()
                .name("review_agent")
                .description("Handle review summary, reputation and score analysis.")
                .instruction(REVIEW_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(reviewTools, shopTools, blogTools)
                .build();
    }
}