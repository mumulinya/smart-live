package com.smartLive.ai.strategy.merchant.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.MerchantAiSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import com.smartLive.ai.service.merchant.IMerchantAiSessionService;
import com.smartLive.ai.service.merchant.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文案 AI 策略类。
 */
@Component("copywriteStrategy")
public class CopywriteAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;
    private final RemoteProductService remoteProductService;

    /**
     * 构造文案 AI 策略类。
     */
    public CopywriteAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                               IMerchantAiSessionService merchantSessionService,
                               IMerchantAiMessageService merchantMessageService,
                               MerchantMessageChatMemoryManager memoryManager,
                               RemoteShopService remoteShopService,
                               IShopRagService shopRagService,
                               ObjectMapper objectMapper,
                               IReviewRagService reviewRagService,
                               RemoteProductService remoteProductService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager,
                remoteShopService, shopRagService, objectMapper);
        this.reviewRagService = reviewRagService;
        this.remoteProductService = remoteProductService;
    }

    /**
     * 校验场景入参。
     */
    @Override
    protected void validateSceneInput(MerchantChatDTO dto, MerchantAiSession session) {
        if (dto.getProductId() == null) {
            throw new ServiceException("Product id is required");
        }
    }

    /**
     * 构建场景提示词。
     */
    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, MerchantAiSession session) {
        ProductDTO product = getAndCheckProduct(dto, session);
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());
        List<ReviewVO> reviews = reviewRagService.searchReviews(product.getName(), session.getShopId());

        return """
                SCENE: PRODUCT_COPYWRITE

                You are generating product marketing copy for the merchant.

                Shop:
                - name: %s
                - type: %s

                Product:
                - id: %s
                - name: %s
                - subtitle: %s
                - sellingPoints: %s
                - activityType: %s
                - rules: %s
                - price: %s
                - originalPrice: %s
                - sold: %s
                - stock: %s

                Related review references:
                %s

                Extra instruction:
                %s

                Requirements:
                1. Output language: Simplified Chinese.
                2. Write one concise product copy only.
                3. Length: 40 to 120 Chinese characters.
                4. Highlight real selling points and fit the product activity type.
                5. Do not invent benefits, ingredients, discounts or stock urgency.
                6. Do not output JSON or markdown.
                """.formatted(
                shopContext.getShopName(),
                shopContext.getShopType(),
                product.getId(),
                defaultText(product.getName()),
                defaultText(product.getSubTitle()),
                resolveSellingPoints(product, reviews),
                resolveActivityType(product.getActivityType()),
                defaultText(product.getRulesJson()),
                product.getPrice() == null ? "0" : product.getPrice().stripTrailingZeros().toPlainString(),
                product.getOriginalPrice() == null ? "0" : product.getOriginalPrice().stripTrailingZeros().toPlainString(),
                defaultNumber(product.getSold()),
                defaultNumber(product.getStock()),
                buildReviewReferences(reviews),
                resolveInstruction(dto)
        );
    }

    /**
     * 获取并校验商品。
     */
    private ProductDTO getAndCheckProduct(MerchantChatDTO dto, MerchantAiSession session) {
        ProductDTO product = remoteProductService.getProductById(dto.getProductId());
        if (product == null) {
            throw new ServiceException("Product not found");
        }
        if (!String.valueOf(session.getShopId()).equals(product.getShopId())) {
            throw new ServiceException("Product and shop mismatch");
        }
        return product;
    }

    /**
     * 解析卖点。
     */
    private String resolveSellingPoints(ProductDTO product, List<ReviewVO> reviews) {
        if (StringUtils.hasText(product.getSubTitle())) {
            return product.getSubTitle().trim();
        }
        List<String> highlights = new ArrayList<>();
        for (ReviewVO review : reviews) {
            if (review == null || !StringUtils.hasText(review.getContent())) {
                continue;
            }
            highlights.add(review.getContent().trim());
            if (highlights.size() >= 3) {
                break;
            }
        }
        return highlights.isEmpty() ? "No data" : String.join("; ", highlights);
    }

    /**
     * 解析活动类型。
     */
    private String resolveActivityType(Integer activityType) {
        if (activityType == null) {
            return "No data";
        }
        return activityType == 0 ? "voucher" : "group-buy product";
    }

    /**
     * 构建评价引用信息。
     */
    private String buildReviewReferences(List<ReviewVO> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "No review reference";
        }
        List<String> lines = new ArrayList<>();
        for (ReviewVO review : reviews) {
            if (review == null || !StringUtils.hasText(review.getContent())) {
                continue;
            }
            lines.add("- " + review.getContent().trim());
            if (lines.size() >= 5) {
                break;
            }
        }
        return lines.isEmpty() ? "No review reference" : String.join("\n", lines);
    }
}
