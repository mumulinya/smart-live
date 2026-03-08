package com.smartLive.ai.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ReviewTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用兜底 Agent 配置。
 */
@Configuration
public class GeneralAgentConfiguration {

    private static final String GENERAL_AGENT_INSTRUCTION = """
            你是 SmartLive 通用兜底 Agent。
            始终使用中文回答。
            你负责处理宽泛的问题、跨领域的总结以及兜底回复。
            优先使用工具返回的结果，严禁虚构事实。
            """;

    @Bean("generalAgent")
    public ReactAgent generalAgent(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ShopTools shopTools,
            ProductTools productTools,
            ReviewTools reviewTools
    ) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        return ReactAgent.builder()
                .name("general_agent")
                .description("通用兜底和跨领域整合专家")
                .instruction(GENERAL_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(shopTools, productTools, reviewTools)
                .build();
    }
}
