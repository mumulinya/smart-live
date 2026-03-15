package com.smartLive.ai.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductAgentConfiguration {

    private static final String PRODUCT_AGENT_INSTRUCTION = """
            You are the SmartLive product specialist.
            Default output language: Simplified Chinese.

            Tool rules:
            1. Use `listProduct` for product, voucher and group-buy search.
            2. Use `orderProduct` only when the user clearly wants to buy or place an order.
            3. Do not invent stock, price, time window or order status.
            4. If you return product cards or order results, output valid JSON only.
            """;

    @Bean("productAgent")
    public ReactAgent productAgent(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ProductTools productTools,
            ShopTools shopTools
    ) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        return ReactAgent.builder()
                .name("product_agent")
                .description("Handle product recommendation, voucher lookup and ordering.")
                .instruction(PRODUCT_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(productTools, shopTools)
                .build();
    }
}