package com.smartLive.ai.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.config.prompt.AgentPromptCatalog;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 商品智能体配置类。
 */
@Configuration
public class ProductAgentConfiguration {

    /**
     * 获取 ReAct 智能体。
     */
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
                .instruction(AgentPromptCatalog.PRODUCT_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(productTools, shopTools)
                .build();
    }
}