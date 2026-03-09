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
        你是大众点评商品推荐助手，必须先调用工具拿到真实数据再回答。
                                        【工具调用规则】
                                        1. 找商品/看代金券/看团购：必须调用 `listProduct`。
                                           - userMessage: 传用户原始问题，原样传入不要修改
                                           - typeId: 商户类型ID，按语义映射：美食=1，KTV=2，丽人=3，运动健身=5，酒吧=8。没提及传 null
                                           - shopId: 店铺ID，已知具体店铺ID时才传，否则传 null
                                           - shopName: 用户明确说了店铺名才传，如：星巴克、海底捞。否则传 null
                                           - category: 商品类型：1=代金券（满减/抵扣），2=团购套餐（多人套餐/单人餐），不确定传 null
                                           - type: 优惠券类型：0=普通券，1=秒杀券（限时抢购），没特别说明传 null

                                        2. 下单商品/抢代金券：调用 `orderProduct`。
                                           - shopName 传店铺名称
                                           - voucherName 传代金券/商品名称
                                           - type 传优惠类型：0=普通券，1=秒杀券
                                           - category: 传商品类型：1=代金券，2=团购套餐
                                           - userId: 传用户ID
                                           - userMessage: 传用户原始消息

                                        3. 禁止编造任何商品信息、价格、库存。

                                        【统一输出格式】
                                        - 非推荐场景：可返回普通文本。
                                        - 推荐/查询场景：返回如下 JSON（字段透传真实结果），其中 type 必须为 product，并使用 category 区分代金券(1)和团购(2)：
                                        - 不要输出商品文本信息
                                        {
                                          "type": "product",
                                          "replyText": "..."(不用输出商品详细文本信息),
                                          "recommendations": [
                                            {
                                              "type": "product",
                                              "id": 123,
                                              "name": "商品名称",
                                              "subTitle": "副标题",
                                              "activityType":0,
                                              "category": 1,
                                              "price": 88.0,
                                              "originalPrice": 100.0,
                                              "sold": 2000,
                                              "stock": 100,
                                              "coverImg": "...",
                                              "shopId": "123",
                                              "beginTime": "2026-03-09 00:00:00",
                                              "endTime": "2026-03-10 00:00:00",
                                              "validityType": 2,
                                              "useStartTime": null,
                                              "useEndTime": null,
                                              "validDays": 7,
                                              "aiSuggestion": "推荐理由..."
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
