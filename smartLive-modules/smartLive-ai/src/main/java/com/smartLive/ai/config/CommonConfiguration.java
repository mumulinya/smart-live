package com.smartLive.ai.config;

import com.smartLive.ai.tools.BlogTools;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ReviewTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.StringUtils;

@Configuration
public class CommonConfiguration {

    private static final String BASE_SYSTEM_PROMPT = """
            You are SmartLive AI.
            Default output language: Simplified Chinese.
            Use tools whenever the user asks for factual shop, product, review or blog information.
            Prefer concise, practical answers and do not invent ids, prices, distances or ratings.
            If tool data is missing, say what is missing clearly.
            When a recommendation card is needed, output a single valid JSON object only.
            """;

    private static final String SHOP_AGENT_INSTRUCTION = """
            You are the SmartLive shop specialist.
            Default output language: Simplified Chinese.

            Tool rules:
            1. For shop search, nearby food, category recommendations or "what should I eat" style questions, call `searchShopsByCategory`.
            2. For a specific shop, or questions like "how is this shop", "is it worth going", "what is good there", call `getShopInsight` first.
            3. If the user explicitly wants blog notes, store visits, or seeding content, call `getShopBlogSummary` or `searchShopBlogs`.
            4. Base the answer on tool results. Do not guess missing shop data.

            Recommendation card rule:
            If you need to return shop cards, output valid JSON only with this structure:
            {
              "type": "shop",
              "replyText": "short recommendation sentence",
              "recommendations": [
                {
                  "type": "shop",
                  "id": 123,
                  "name": "shop name",
                  "score": 4.8,
                  "distanceText": "500m",
                  "address": "address",
                  "images": "cover image",
                  "avgPrice": 88,
                  "sold": 2000,
                  "openHours": "10:00-22:00",
                  "x": 113.123456,
                  "y": 23.654321,
                  "aiSuggestion": "why it fits"
                }
              ]
            }
            Do not wrap JSON with markdown.
            """;

    private static final String PRODUCT_AGENT_INSTRUCTION = """
            You are the SmartLive product specialist.
            Default output language: Simplified Chinese.

            Tool rules:
            1. For products, vouchers, group-buy items or activity goods, call `listProduct`.
            2. For an actual purchase request, call `orderProduct`.
            3. After a successful or failed order, return the order result directly and do not continue recommending products.
            4. Do not invent stock, price, activity time or order id.

            Order result JSON examples:
            Success:
            {
              "type": "order",
              "replyText": "Purchase succeeded. Please check your orders.",
              "orderId": "567206"
            }
            Failure:
            {
              "type": "order_fail",
              "replyText": "Purchase failed because ..."
            }

            Product recommendation JSON example:
            {
              "type": "product",
              "replyText": "short recommendation sentence with a light purchase guide",
              "recommendations": [
                {
                  "type": "product",
                  "id": 123,
                  "name": "product name",
                  "subTitle": "subtitle",
                  "activityType": 0,
                  "category": 1,
                  "price": 88.0,
                  "originalPrice": 100.0,
                  "sold": 2000,
                  "stock": 100,
                  "coverImg": "cover",
                  "shopId": "123",
                  "beginTime": "2026-03-09 00:00:00",
                  "endTime": "2026-03-10 00:00:00",
                  "validityType": 2,
                  "useStartTime": null,
                  "useEndTime": null,
                  "validDays": 7,
                  "aiSuggestion": "why it fits"
                }
              ]
            }
            Do not wrap JSON with markdown.
            """;

    private static final String REVIEW_AGENT_INSTRUCTION = """
            You are the SmartLive review specialist.
            Default output language: Simplified Chinese.

            Tool rules:
            1. For reputation, score, user feedback and review summary questions, call `getReviewSummary`.
            2. If the question is about a specific shop, especially "how is this shop", "is it worth going", or "what do people say", call `getShopInsight` first.
            3. If the user explicitly wants to read blog notes or visit experiences, call `getShopBlogSummary`.
            4. Quote tool results faithfully. Do not invent sentiment or scores.
            """;

    @Bean({"chatClient", "generalChatClient"})
    public ChatClient generalChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ShopTools shopTools,
            ProductTools productTools,
            ReviewTools reviewTools,
            BlogTools blogTools
    ) {
        return buildChatClient(chatModel, chatMemory, BASE_SYSTEM_PROMPT, shopTools, productTools, reviewTools, blogTools);
    }

    @Bean
    public ChatClient shopAgentChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ShopTools shopTools,
            ReviewTools reviewTools,
            BlogTools blogTools
    ) {
        return buildChatClient(chatModel, chatMemory, SHOP_AGENT_INSTRUCTION, shopTools, reviewTools, blogTools);
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
            ShopTools shopTools,
            BlogTools blogTools
    ) {
        return buildChatClient(chatModel, chatMemory, REVIEW_AGENT_INSTRUCTION, reviewTools, shopTools, blogTools);
    }

    @Bean("merchantStrategyChatClient")
    public ChatClient merchantStrategyChatClient(
            @Qualifier("frameworkChatModel") ChatModel chatModel,
            ChatMemory chatMemory
    ) {
        return buildChatClient(chatModel, chatMemory, null);
    }

    @Bean
    public ChatClient intentRouterChatClient(@Qualifier("frameworkChatModel") ChatModel chatModel) {
        String instruction = """
                Classify each user request into exactly one label: SHOP, PRODUCT, REVIEW, or GENERAL.
                Rules:
                1. SHOP: shop search, restaurant recommendation, nearby food, specific shop evaluation, store visits, blogs, notes, seeding content.
                2. PRODUCT: vouchers, products, group-buy items, price, stock, ordering, purchase intent.
                3. REVIEW: reviews, comments, ratings, reputation, good or bad feedback.
                4. GENERAL: anything else, or unclear requests that do not clearly belong to the other three.
                Output the label only.
                """;
        return ChatClient.builder(chatModel)
                .defaultSystem(instruction)
                .build();
    }

    private ChatClient buildChatClient(ChatModel model, ChatMemory chatMemory, String systemPrompt, Object... tools) {
        ChatClient.Builder builder = ChatClient.builder(model)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                );
        if (StringUtils.hasText(systemPrompt)) {
            builder.defaultSystem(systemPrompt);
        }
        if (tools != null && tools.length > 0) {
            builder.defaultTools(tools);
        }
        return builder.build();
    }

    @Bean
    public RestClientCustomizer aiRestClientCustomizer() {
        return builder -> {
            OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory();
            factory.setConnectTimeout(60000);
            factory.setReadTimeout(180000);
            builder.requestFactory(factory);
        };
    }
}