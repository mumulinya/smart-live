package com.smartLive.ai.strategy.merchant.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
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

@Component("copywriteStrategy")
public class CopywriteAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;
    private final RemoteProductService remoteProductService;

    public CopywriteAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                               IAiMerchantSessionService merchantSessionService,
                               IAiMerchantMessageService merchantMessageService,
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

    @Override
    protected void validateSceneInput(MerchantChatDTO dto, AiMerchantSession session) {
        if (dto.getProductId() == null) {
            throw new ServiceException("Product id is required");
        }
    }

    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, AiMerchantSession session) {
        ProductDTO product = getAndCheckProduct(dto, session);
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());
        List<ReviewVO> reviews = reviewRagService.searchReviews(product.getName(), session.getShopId());

        return """
                场景 2：商品文案 PRODUCT_COPYWRITE

                当前任务：根据商品信息生成一段可直接使用的营销文案。

                【店铺信息】
                店铺名称：%s
                店铺类型：%s

                【商品信息】
                商品ID：%s
                商品名称：%s
                商品副标题：%s
                商品卖点：%s
                活动类型：%s
                活动规则：%s
                售价：%s
                原价：%s
                累计销量：%s
                库存：%s

                【评价参考】
                %s

                【商家补充要求】
                %s

                【输出要求】
                1. 只输出最终文案正文，不要标题，不要解释。
                2. 字数控制在 40-120 字。
                3. 突出商品核心卖点、适用场景和购买吸引力。
                4. 语气要有营销感，但不要夸大和虚假承诺。
                5. 不得编造功效、材质、产地、官方认证、销量数据或优惠力度。
                6. 如果有价格或活动信息，可以自然融入，但不要写得像硬广口播。
                7. 若商家补充了风格要求，如“活泼”“高级感”“专业”，优先按该风格输出。
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

    private ProductDTO getAndCheckProduct(MerchantChatDTO dto, AiMerchantSession session) {
        ProductDTO product = remoteProductService.getProductById(dto.getProductId());
        if (product == null) {
            throw new ServiceException("Product not found");
        }
        if (!String.valueOf(session.getShopId()).equals(product.getShopId())) {
            throw new ServiceException("Product and shop mismatch");
        }
        return product;
    }

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
        return highlights.isEmpty() ? "暂无数据" : String.join("；", highlights);
    }

    private String resolveActivityType(Integer activityType) {
        if (activityType == null) {
            return "暂无数据";
        }
        return activityType == 0 ? "普通商品" : "活动商品";
    }

    private String buildReviewReferences(List<ReviewVO> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "暂无评价参考";
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
        return lines.isEmpty() ? "暂无评价参考" : String.join("\n", lines);
    }
}