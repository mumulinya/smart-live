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
 * Voucher handler.
 */
@Slf4j
@Service
public class ProductHandler implements ChatHandler {

    @Autowired
    private AIClient aiClient;

    private static final List<String> PRODUCT_KEYWORDS = Arrays.asList(
            "商品", "秒杀", "团购", "价格", "优惠", "买", "卖", "多少钱"
    );

    @Override
    public String getHandlerType() {
        return "product";
    }

    @Override
    public boolean canHandle(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        return PRODUCT_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    @Override
    public Flux<String> handle(AIChatRequest request) {
        log.info("ProductHandler processing message: {}", request.getMessage());
        try {
            String prompt = buildPrompt(request);
            return aiClient.generate(prompt, request.getSessionId());
        } catch (Exception e) {
            log.error("VoucherHandler failed", e);
            return Flux.just("抱歉，我暂时无法处理这个代金券请求。");
        }
    }

    @Override
    public String buildPrompt(AIChatRequest request) {
        String promptTemplate = """
                你是本地生活代金券助手，必须先调用工具拿到真实数据再回答。

                【用户输入】
                - message: %s
                - userId: %s

                【工具调用规则】
                1. 查券场景：必须调用 `listVoucher`。
                   - type：秒杀/限时/抢购 -> 1；普通券/代金券 -> 0；不明确可不传
                   - shopName：用户提到店名时必须传
                   - typeId：用户提到品类时传入
                   - userMessage：必须传原始问题

                2. 下单场景：必须调用 `orderVoucher`。
                   - 用户明确表达下单/购买/抢时才调用
                   - 能提取 shopId 优先传 shopId；否则传 shopName + voucherName
                   - type/userId/userMessage 尽量传齐

                3. 禁止编造任何代金券信息。

                【统一输出格式】
                - 非推荐场景：可返回普通文本。
                - 推荐场景：严格返回如下 JSON（字段透传真实结果）
                {
                  "type": "voucher",
                  "replyText": "...",
                  "recommendations": [
                    {
                      "type": "voucher",
                      "id": 13,
                      "shopId": 8,
                      "shopName": "店铺名",
                      "title": "100元代金券",
                      "subTitle": "周一至周五可用",
                      "payValue": "80",
                      "actualValue": 100,
                      "voucherType": 0,
                      "stock": 222,
                      "beginTime": "2026-02-14T10:00:00",
                      "endTime": "2026-02-20T23:59:59",
                      "rules": "...",
                      "aiSuggestion": "..."
                    }
                  ]
                }
                """;

        String message = safe(request.getMessage());
        String userId = safe(request.getUserId());
        String prompt = String.format(promptTemplate, message, userId);
        log.info("Voucher prompt generated为{}", prompt);
        return prompt;
    }

    private String safe(String value) {
        return value == null ? "null" : value;
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
