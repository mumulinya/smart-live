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
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 回复 AI 策略类。
 */
@Component("replyStrategy")
public class ReplyAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;
    private final RemoteOrderService remoteOrderService;

    /**
     * 构造回复 AI 策略类。
     */
    public ReplyAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                           IMerchantAiSessionService merchantSessionService,
                           IMerchantAiMessageService merchantMessageService,
                           MerchantMessageChatMemoryManager memoryManager,
                           RemoteShopService remoteShopService,
                           IShopRagService shopRagService,
                           ObjectMapper objectMapper,
                           IReviewRagService reviewRagService,
                           RemoteOrderService remoteOrderService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager,
                remoteShopService, shopRagService, objectMapper);
        this.reviewRagService = reviewRagService;
        this.remoteOrderService = remoteOrderService;
    }

    /**
     * 校验场景入参。
     */
    @Override
    protected void validateSceneInput(MerchantChatDTO dto, MerchantAiSession session) {
        if (dto.getReviewId() == null) {
            throw new ServiceException("Review id is required");
        }
    }

    /**
     * 构建场景提示词。
     */
    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, MerchantAiSession session) {
        ReviewVO review = reviewRagService.getReviewById(dto.getReviewId(), session.getShopId());
        if (review == null) {
            throw new ServiceException("Review not found");
        }
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());
        Integer consumeCount = review.getUserId() == null ? 0 : remoteOrderService.getOrderCount(review.getUserId());
        if (consumeCount == null) {
            consumeCount = 0;
        }

        return """
                SCENE: REVIEW_REPLY

                You are replying to a customer review on behalf of the merchant.

                Shop:
                - name: %s
                - type: %s

                Review:
                - id: %s
                - score: %s
                - createdAt: %s
                - sourceType: %s
                - sourceName: %s
                - content: %s

                Customer:
                - nickname: %s
                - orderCount: %s

                Extra instruction:
                %s

                Requirements:
                1. Output language: Simplified Chinese.
                2. Write one direct merchant reply only.
                3. Length: 30 to 80 Chinese characters.
                4. If score is 4-5, thank the customer and reinforce the positive points.
                5. If score is 3, acknowledge the issue and show willingness to improve.
                6. If score is 1-2, apologize sincerely, address the problem and provide a calm follow-up tone.
                7. Do not argue with the customer and do not invent compensation details.
                8. Do not output JSON or markdown.
                """.formatted(
                shopContext.getShopName(),
                shopContext.getShopType(),
                defaultLongNumber(review.getId()),
                defaultNumber(review.getScore()),
                formatDate(review.getCreateTime()),
                resolveSourceTypeName(review.getSourceType()),
                defaultText(review.getSourceName()),
                defaultText(review.getContent()),
                defaultText(review.getNickName()),
                defaultNumber(consumeCount),
                resolveInstruction(dto)
        );
    }

    /**
     * 解析来源类型名称。
     */
    private String resolveSourceTypeName(Integer sourceType) {
        if (sourceType == null) {
            return "unknown";
        }
        if (GlobalBizTypeEnum.SHOP.getCode().equals(sourceType)) {
            return "shop";
        }
        if (GlobalBizTypeEnum.PRODUCT.getCode().equals(sourceType)) {
            return "product";
        }
        if (GlobalBizTypeEnum.REVIEW.getCode().equals(sourceType)) {
            return "review";
        }
        return "other";
    }
}
