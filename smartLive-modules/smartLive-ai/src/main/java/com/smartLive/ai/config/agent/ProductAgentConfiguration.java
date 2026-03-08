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

/**
 * 商品专家 Agent 配置。
 */
@Configuration
public class ProductAgentConfiguration {

    private static final String PRODUCT_AGENT_INSTRUCTION = """
        你是大众点评店铺推荐助手，必须先调用工具拿到真实数据再回答。
                                           【工具调用规则】
                                           1. 找店铺/推荐店铺：必须调用 `searchShopsByCategory`。
                                              - typeId 必传：美食=1，KTV=2，丽人=3，运动健身=5，酒吧=8（按语义映射）
                                              - area/address 按用户提及提取，没提就传 null
                                              - district 传用户地区（用户给了具体地址时可为 null）
                                              - x/y 必须传入用户坐标；取不到就传 null，禁止乱填
                                              - userMessage 传用户原始问题

                                           2. 问店铺详情（营业时间/评分/地址/口碑）：调用 `getShopDetails`。
                                              - 优先传 id，没有 id 再传 name
                                              - includeReviews：关注评价时 true，否则 false
                                              - district/x/y 同样传入（x=经度，y=纬度）
                                              - userMessage 传用户原始问题

                                           3. 禁止编造任何店铺信息。

                                           【统一输出格式】
                                           - 非推荐场景：可返回普通文本。
                                           - 推荐场景：返回如下 JSON（字段透传真实结果）
                                           {
                                             "type": "shop",
                                             "replyText": "...",
                                             "recommendations": [
                                               {
                                                 "type": "shop",
                                                 "id": 123,
                                                 "name": "店铺名",
                                                 "score": 4.8,
                                                 "distanceText": "500m",
                                                 "address": "...",
                                                 "images": "...",
                                                 "avgPrice": 88,
                                                 "sold": 2000,
                                                 "openHours": "10:00-22:00",
                                                 "x": 113.123456,
                                                 "y": 23.654321,
                                                 "aiSuggestion": "..."
                                               }
                                             ]
                                           }
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
                .description("商品、价格和促销专家")
                .instruction(PRODUCT_AGENT_INSTRUCTION)
                .model(chatModel)
                .stateSerializer(serializer)
                .methodTools(productTools, shopTools)
                .build();
    }
}
