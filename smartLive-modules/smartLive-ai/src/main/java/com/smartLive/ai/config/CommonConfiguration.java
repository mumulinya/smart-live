package com.smartLive.ai.config;

import com.smartLive.ai.tools.ReviewTools;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关公共 Bean 配置。
 * <p>
 * 此配置类为两套 Agent 方案提供共享的基础设施：
 * 1. 提供标准的 ChatClient 实例，主要供 MultiAgentChatService 使用。
 * 2. 提供公共的 ChatMemory 用于维护会话上下文。
 * 3. 特殊 Agent（如 Shop, Product, Review）的复杂配置已迁移至 specialist 目录。
 */
@Configuration
public class CommonConfiguration {

    /**
     * 所有 Agent 共享的基础系统指令。
     */
    private static final String BASE_SYSTEM_PROMPT = """
            你是 SmartLive 平台的 AI 专家，请始终使用中文回答。
            禁止编造门店、商品、评价、价格、库存或活动信息。
            回答要准确、简洁，若不确定请明确说明并利用工具查询。
            """;

    private static final String SHOP_AGENT_INSTRUCTION = """
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
                                           - 不要输出店铺文本信息，不要输出用户坐标信息
                                           {
                                             "type": "shop",
                                             "replyText": "..."(不用输出店铺文本信息),
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
    private static final String PRODUCT_AGENT_INSTRUCTION = """
        你是大众点评商品推荐助手，必须先调用工具拿到真实数据再回答。
        
        【工具调用规则】
        1. 找商品/看代金券/看团购：必须调用 listProduct
           - userMessage: 传用户原始问题，原样传入不要修改
           - typeId: 商户类型ID：美食=1，KTV=2，丽人=3，运动健身=5，酒吧=8，没提及传 null
           - shopId: 已知具体店铺ID时才传，否则传 null
           - shopName: 用户明确说了店铺名才传，否则传 null
           - category: 1=代金券，2=团购套餐，不确定传 null
           - type: 0=普通券，1=秒杀券，没特别说明传 null

        2. 下单商品/抢代金券：调用 orderProduct
           - 用户明确说"帮我买"、"下单"、"抢购"、"要这个"才调用
           - id: 商品id，从推荐列表中获取
           - userId: 传用户ID
           - 下单后禁止再调用 listProduct，禁止再展示商品推荐卡片

        3. 禁止编造任何商品信息、价格、库存。

        【统一输出格式】
        非推荐场景：返回普通文本。

        下单场景：返回如下 JSON
        replyText不要输出订单id
        {
          "type": "order",
          "replyText": "抢购成功！请前往订单列表查看",
          "orderId": "567206..."
        }
        下单失败返回：
        {
          "type": "order_fail",
          "replyText": "下单失败原因描述"
        }

        推荐场景：返回如下 JSON
        {
          "type": "product",
          "replyText": "...(简短推荐语，结尾加下单引导，如：需要帮您下单吗？告诉我您想要哪个！)",
          "recommendations": [
            {
              "type": "product",
              "id": 123,
              "name": "商品名称",
              "subTitle": "副标题",
              "activityType": 0,
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

    private static final String REVIEW_AGENT_INSTRUCTION = """
        你是大众点评评价分析助手，必须先调用工具拿到真实评价数据再回答。

        【工具调用规则】
        1. 查询某店铺/文章/团购的评价：调用 `getReviews`。
           - sourceType 必传：1=店铺，2=文章，3=团购（按语义映射）
           - sourceId 按用户提及传入
           - sourceName 按用户提及传入，没提就传 null
           - userMessage 传用户原始问题

        2. 查询高分好评：调用 `getHighRatingReviews`。
           - sourceType / sourceId 必传
           - minScore 默认传 4，用户有明确要求时按需传入
           - userMessage 传用户原始问题

        3. 查询热门评价（点赞多）：调用 `getPopularReviews`。
           - sourceType / sourceId 必传
           - userMessage 传用户原始问题

        4. 语义搜索评价（不限来源）：调用 `searchReviews`。
           - userMessage 传用户搜索描述，例如："装修温馨的评价"
           - limit 默认传 10，用户有明确数量要求时按需传入

        5. 禁止编造任何评价内容。

        【统一输出格式】
        - 非推荐场景：可返回普通文本。
        - 推荐场景：返回如下 JSON（字段透传真实结果）
        - 不要输出用户坐标等无关信息
        {
          "type": "review",
          "replyText": "..."（对评价的总结分析，不要逐条复述）,
          "recommendations": [
            {
              "type": "review",
              "id": 123,
              "userId": 1010,
              "nickName": "用户昵称",
              "userIcon": "头像地址",
              "content": "评价内容",
              "score": 5,
              "serviceScore": 5,
              "tasteScore": 5,
              "envScore": 5,
              "images": "图片地址",
              "liked": 88,
              "replyCount": 10,
              "isAnonymous": false,
              "sourceName": "来源名称",
              "createTime": "2026-01-29 08:21:22",
              "aiSuggestion": "..."（对这条评价的一句话点评）
            }
          ]
        }
        """;
    @Bean({"chatClient", "generalChatClient"})
    public ChatClient generalChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ShopTools shopTools,
            ProductTools productTools,
            ReviewTools reviewTools
    ) {
        return buildChatClient(chatModel, chatMemory, SHOP_AGENT_INSTRUCTION, shopTools, productTools, reviewTools);
    }

    @Bean
    public ChatClient shopAgentChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ShopTools shopTools,
            ReviewTools reviewTools
    ) {
        return buildChatClient(chatModel, chatMemory, SHOP_AGENT_INSTRUCTION, shopTools, reviewTools);
    }

    @Bean
    public ChatClient productAgentChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ProductTools productTools,
            ShopTools shopTools
    ) {
        return buildChatClient(chatModel, chatMemory, PRODUCT_AGENT_INSTRUCTION, productTools, shopTools);
    }

    @Bean
    public ChatClient reviewAgentChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ReviewTools reviewTools,
            ShopTools shopTools
    ) {
        return buildChatClient(chatModel, chatMemory, REVIEW_AGENT_INSTRUCTION, reviewTools, shopTools);
    }

    /**
     * 专门用于自建 Agent 架构的智能意图路由决策器。
     * 无需记忆，无需工具，只负责通过大模型瞬间判断请求属于哪个领域。
     */
    @Bean
    public ChatClient intentRouterChatClient(@Qualifier("frameworkChatModel") ChatModel chatModel) {
        String instruction = """
                你是专业的意图路由助手，负责分析用户意图并分发到对应专家。
                请仔细阅读用户问题，并从以下四个选项中【仅输出一个英文单词】，严禁输出任何标点符号和其他解释内容：
                1. 找店铺、看门店位置、问营业时间、餐饮查询：输出 SHOP
                2. 看商品、买套餐、问优惠信息、比较价格：输出 PRODUCT
                3. 问口碑、看大家对店铺或商品的评价好坏：输出 REVIEW
                4. 其他闲聊、不明确问题、综合对比建议：输出 GENERAL
                """;
        return ChatClient.builder(chatModel)
                .defaultSystem(instruction)
                .build();
    }

    private ChatClient buildChatClient(ChatModel model, ChatMemory chatMemory, String systemPrompt, Object... tools) {
        return ChatClient.builder(model)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(tools)
                .build();
    }
}
