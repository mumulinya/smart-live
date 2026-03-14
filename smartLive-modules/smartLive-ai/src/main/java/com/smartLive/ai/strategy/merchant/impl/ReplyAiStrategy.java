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
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("replyStrategy")
public class ReplyAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;
    private final RemoteOrderService remoteOrderService;

    public ReplyAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                           IAiMerchantSessionService merchantSessionService,
                           IAiMerchantMessageService merchantMessageService,
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

    @Override
    protected void validateSceneInput(MerchantChatDTO dto, AiMerchantSession session) {
        if (dto.getReviewId() == null) {
            throw new ServiceException("Review id is required");
        }
    }

    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, AiMerchantSession session) {
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
                场景 1：评价回复 REVIEW_REPLY

                当前任务：为商家生成一条可直接发布的评价回复。

                【店铺信息】
                店铺名称：%s
                店铺类型：%s

                【评价信息】
                评价ID：%s
                评分：%s
                评价时间：%s
                评价对象类型：%s
                评价对象名称：%s
                评价内容：%s

                【用户信息】
                昵称：%s
                用户标签：暂无数据
                历史消费次数：%s

                【商家补充要求】
                %s

                【输出要求】
                1. 只输出最终回复正文，不要标题，不要“建议回复”，不要加引号。
                2. 字数控制在 30-80 字。
                3. 评分 4-5 分时，以感谢、认可、欢迎再次光临为主。
                4. 评分 3 分时，兼顾感谢与改进态度。
                5. 评分 1-2 分时，先真诚致歉，再表达重视和后续改进态度。
                6. 如果评价提到具体问题，回复中要轻微回应该问题，但不要逐字复述差评内容。
                7. 不得承诺退款、赔偿、赠品、私下联系方式，除非数据中明确提供。
                8. 优先使用“我们”，语气自然、像真人商家回复。
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

    private String resolveSourceTypeName(Integer sourceType) {
        if (sourceType == null) {
            return "暂无数据";
        }
        if (GlobalBizTypeEnum.SHOP.getCode().equals(sourceType)) {
            return "店铺";
        }
        if (GlobalBizTypeEnum.PRODUCT.getCode().equals(sourceType)) {
            return "商品";
        }
        if (GlobalBizTypeEnum.REVIEW.getCode().equals(sourceType)) {
            return "评价";
        }
        return "其他";
    }
}