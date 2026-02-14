package com.smartLive.ai.strategy.handlers;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.ai.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * Shop search handler.
 */
@Slf4j
@Service
public class ShopSearchHandler implements ChatHandler {

    @Autowired
    private AIClient aiClient;

    private static final List<String> SEARCH_KEYWORDS = Arrays.asList(
            "附近", "搜索", "找", "哪里有", "在哪", "位置", "地址", "怎么去", "怎么走",
            "美食", "饭店", "餐厅", "火锅", "奶茶", "店铺"
    );

    @Override
    public String getHandlerType() {
        return "search";
    }

    @Override
    public boolean canHandle(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase();
        return SEARCH_KEYWORDS.stream().anyMatch(normalized::contains)
                || message.matches(".*\\d+\\s*(公里|千米|米|分钟).*");
    }

    @Override
    public Flux<String> handle(AIChatRequest request) {
        log.info("ShopSearchHandler processing message: {}", request.getMessage());

        try {
            String prompt = buildPrompt(request);
            return aiClient.generate(prompt, request.getSessionId());
        } catch (Exception e) {
            log.error("ShopSearchHandler failed", e);
            return Flux.just("抱歉，我暂时无法处理这个店铺查询请求。");
        }
    }

    @Override
    public String buildPrompt(AIChatRequest request) {
        String promptTemplate = """
                你是大众点评店铺推荐助手，必须先调用工具拿到真实数据再回答。

                【用户输入】
                - message: %s
                - district: %s
                - x(longitude): %s
                - y(latitude): %s

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

        String message = safe(request.getMessage());
        String district = safe(request.getDistrict());
        String x = request.getX() == null ? "null" : String.valueOf(request.getX());
        String y = request.getY() == null ? "null" : String.valueOf(request.getY());

        String fullPrompt = String.format(promptTemplate, message, district, x, y);
        log.info("Shop prompt generated为{}", fullPrompt);
        return fullPrompt;
    }

    private String safe(String value) {
        return value == null ? "null" : value;
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
