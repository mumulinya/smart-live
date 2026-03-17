package com.smartLive.ai.strategy.merchant.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.MerchantAiSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import com.smartLive.ai.service.merchant.IMerchantAiSessionService;
import com.smartLive.ai.service.merchant.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.shop.api.DTO.ProductSalesDTO;
import com.smartLive.shop.api.DTO.ShopSuggestDTO;
import com.smartLive.shop.api.RemoteShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 推荐 AI 策略类。
 */
@Component("suggestStrategy")
@Slf4j
public class SuggestAiStrategy extends AbstractMerchantAiStrategy {

    /**
     * 构造推荐 AI 策略类。
     */
    public SuggestAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                             IMerchantAiSessionService merchantSessionService,
                             IMerchantAiMessageService merchantMessageService,
                             MerchantMessageChatMemoryManager memoryManager,
                             RemoteShopService remoteShopService,
                             IShopRagService shopRagService,
                             ObjectMapper objectMapper) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager,
                remoteShopService, shopRagService, objectMapper);
    }

    /**
     * 校验场景入参。
     */
    @Override
    protected void validateSceneInput(MerchantChatDTO dto, MerchantAiSession session) {
        if (dto == null || dto.getShopSuggestData() == null) {
            throw new ServiceException("shopSuggestData cannot be blank");
        }
    }

    /**
     * 构建场景提示词。
     */
    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, MerchantAiSession session) {
        ShopSuggestDTO suggest = dto.getShopSuggestData();
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());

        return """
                SCENE: OPERATION_SUGGESTION

                You are generating operation improvement suggestions for the merchant.

                Shop:
                - name: %s
                - type: %s

                Current business snapshot:
                - pendingReviewCount: %s
                - badReviewCount: %s
                - repurchaseRate: %s%%
                - slowProducts: %s
                - badReviewKeywords: %s

                Extra instruction:
                %s

                Requirements:
                1. Output language: Simplified Chinese.
                2. Give practical operation suggestions, not generic slogans.
                3. Structure the answer into diagnosis, short-term actions and medium-term improvements.
                4. Prioritize pending reviews, bad reviews, repurchase rate and slow-moving products.
                5. Explain the actions clearly enough for the merchant to execute.
                6. Do not invent facts that are not provided.
                7. Do not output JSON.
                """.formatted(
                shopContext.getShopName(),
                shopContext.getShopType(),
                defaultNumber(suggest.getPendingReviewCount()),
                defaultNumber(suggest.getBadReviewCount()),
                defaultDecimal(suggest.getRepurchaseRate()),
                formatProductSales(suggest.getSlowProducts()),
                formatKeywords(suggest.getBadReviewKeywords()),
                resolveInstruction(dto)
        );
    }

    /**
     * 获取字符串结果。
     */
    private String formatProductSales(List<ProductSalesDTO> products) {
        if (products == null || products.isEmpty()) {
            return "No data";
        }
        List<String> items = new ArrayList<>();
        for (ProductSalesDTO product : products) {
            if (product == null) {
                continue;
            }
            items.add(defaultText(product.getProductName()) + "(sales=" + defaultLongNumber(product.getSalesCount()) + ")");
        }
        return items.isEmpty() ? "No data" : String.join("; ", items);
    }

    /**
     * 获取字符串结果。
     */
    private String formatKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "No data";
        }
        List<String> items = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            items.add(keyword.trim());
        }
        return items.isEmpty() ? "No data" : String.join(", ", items);
    }
}
