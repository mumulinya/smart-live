package com.smartLive.ai.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.config.prompt.AgentPromptCatalog;
import com.smartLive.ai.tools.BlogTools;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ReviewTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用智能体配置类。
 */
@Configuration
public class GeneralAgentConfiguration {

    /**
     * 获取 ReAct 智能体。
     */
    @Bean("generalAgent")
    public ReactAgent generalAgent(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ShopTools shopTools,
            ProductTools productTools,
            ReviewTools reviewTools,
            BlogTools blogTools
    ) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        return ReactAgent.builder()
                .name("general_agent")
                .description("Handle mixed user requests with the full SmartLive toolset.")
                .instruction(AgentPromptCatalog.GENERAL_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(shopTools, productTools, reviewTools, blogTools)
                .build();
    }
}