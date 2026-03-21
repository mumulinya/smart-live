package com.smartLive.ai.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.config.prompt.AgentPromptCatalog;
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
                .instruction(AgentPromptCatalog.SHOP_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(shopTools, reviewTools, blogTools)
                .build();
    }
}