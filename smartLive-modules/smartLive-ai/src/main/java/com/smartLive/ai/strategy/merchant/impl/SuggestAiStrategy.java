package com.smartLive.ai.strategy.merchant.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.MerchantAiSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import com.smartLive.ai.service.merchant.IMerchantAiSessionService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.interaction.api.DTO.BadReviewDTO;
import com.smartLive.interaction.api.DTO.ShopReviewSuggestDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import com.smartLive.shop.api.DTO.ProductSalesDTO;
import com.smartLive.shop.api.DTO.ShopSuggestDTO;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component("suggestStrategy")
public class SuggestAiStrategy extends AbstractMerchantAiStrategy {

    private final RemoteReviewService remoteReviewService;

    public SuggestAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                             IMerchantAiSessionService merchantSessionService,
                             IMerchantAiMessageService merchantMessageService,
                             MerchantMessageChatMemoryManager memoryManager,
                             RemoteShopService remoteShopService,
                             IShopRagService shopRagService,
                             ObjectMapper objectMapper,
                             RemoteReviewService remoteReviewService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager,
                remoteShopService, shopRagService, objectMapper);
        this.remoteReviewService = remoteReviewService;
    }

    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, MerchantAiSession session) {
        String normalizedDateRange = normalizeDateRange(dto.getDateRange());
        ShopSuggestDTO suggest = convertAjaxData(
                remoteShopService.getShopSuggest(session.getShopId(), normalizedDateRange),
                ShopSuggestDTO.class,
                new ShopSuggestDTO()
        );
        ShopReviewSuggestDTO reviewSuggest = remoteReviewService.getShopReviewSuggest(session.getShopId(), normalizedDateRange);
        if (reviewSuggest == null) {
            reviewSuggest = new ShopReviewSuggestDTO();
        }
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());

        return """
                SCENE: OPERATION_SUGGESTION

                You are generating operation suggestions for the merchant.

                Shop:
                - name: %s
                - type: %s

                Current business snapshot:
                - weekOrders: %s
                - pendingReviewCount: %s
                - avgScore: %s
                - badReviewCount: %s
                - hotProducts: %s
                - slowProducts: %s
                - badReviewIssues:
                %s

                Extra instruction:
                %s

                Requirements:
                1. Output language: Simplified Chinese.
                2. Give practical operation suggestions, not generic slogans.
                3. Structure the answer into diagnosis, short-term actions and medium-term improvements.
                4. Prioritize actions that can be executed by the merchant.
                5. Combine pending reviews and bad review issues into the action plan.
                6. Do not invent facts that are not provided.
                7. Do not output JSON.
                """.formatted(
                shopContext.getShopName(),
                shopContext.getShopType(),
                defaultNumber(suggest.getWeekOrders()),
                defaultNumber(suggest.getPendingReviewCount()),
                defaultDecimal(suggest.getAvgScore()),
                defaultNumber(suggest.getBadReviewCount()),
                formatProductSales(suggest.getHotProducts()),
                formatProductSales(suggest.getSlowProducts()),
                formatBadReviewIssues(reviewSuggest.getBadReviewList()),
                resolveInstruction(dto)
        );
    }

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

    private String formatBadReviewIssues(List<BadReviewDTO> badReviewList) {
        if (badReviewList == null || badReviewList.isEmpty()) {
            return "No data";
        }
        List<String> items = new ArrayList<>();
        for (BadReviewDTO review : badReviewList) {
            if (review == null || !StringUtils.hasText(review.getContent())) {
                continue;
            }
            items.add("- [score=" + defaultNumber(review.getScore()) + "][" + formatDate(review.getCreateTime()) + "] " + review.getContent().trim());
            if (items.size() >= 5) {
                break;
            }
        }
        return items.isEmpty() ? "No data" : String.join("\n", items);
    }
}