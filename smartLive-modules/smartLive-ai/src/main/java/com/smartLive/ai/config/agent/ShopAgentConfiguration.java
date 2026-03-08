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
 * 门店专家 Agent 配置。
 */
@Configuration
public class ShopAgentConfiguration {

    private static final String SHOP_AGENT_INSTRUCTION = """
        你是店铺推荐助手，必须先调用工具获取真实数据再回答。

        工具调用规则：
        调用 searchShopsByCategory 搜索店铺
        - typeId：美食=1，KTV=2，丽人=3，运动健身=5，酒吧=8
        - x/y 必须是数字类型，取不到传 null，禁止传文字

        输出规则：
        工具已返回完整 JSON 数据，你只需要做两件事：
        1. 填写 replyText：一句简短推荐语，不包含店铺名单，不包含坐标
        2. 填写每个店铺的 aiSuggestion：一句话推荐理由
        3. 其余所有字段原样保留，禁止修改任何数值
        4. 直接输出 JSON，不加任何多余文字
        """;

    @Bean("shopAgent")
    public ReactAgent shopAgent(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ShopTools shopTools,
            ReviewTools reviewTools
    ) {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

        return ReactAgent.builder()
                .name("shop_agent")
                .description("查询店铺信息")
                .instruction(SHOP_AGENT_INSTRUCTION)
                .model(chatModel)
                .systemPrompt("""
                你只能输出两种格式：
                1. 普通文本（非推荐场景）
                2. 纯 JSON（推荐场景），不加 markdown 代码块，不加任何解释文字
                违反格式视为错误输出。
                """)
                .stateSerializer(serializer)
                .methodTools(shopTools, reviewTools)
                .build();
    }
}
