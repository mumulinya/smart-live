package com.smartLive.ai.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.tools.ReviewTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 评价专家 Agent 配置。
 */
@Configuration
public class ReviewAgentConfiguration {

    private static final String REVIEW_AGENT_INSTRUCTION = """
            你是 SmartLive 评价专家 Agent。
            始终使用中文回答。
            仅关注评价相关话题：情感倾向、评分、优缺点、用户反馈和潜在风险点。
            优先使用工具返回的结果，严禁虚构事实。
            """;

    @Bean("reviewAgent")
    public ReactAgent reviewAgent(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ReviewTools reviewTools,
            ShopTools shopTools
    ) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        return ReactAgent.builder()
                .name("review_agent")
                .description("评价、评分和用户反馈专家")
                .instruction(REVIEW_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(reviewTools, shopTools)
                .build();
    }
}
