package com.smartLive.ai.strategy.merchant.impl;

import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("suggestStrategy")
public class SuggestAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;

    public SuggestAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                             IAiMerchantSessionService merchantSessionService,
                             IAiMerchantMessageService merchantMessageService,
                             MerchantMessageChatMemoryManager memoryManager,
                             IReviewRagService reviewRagService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager);
        this.reviewRagService = reviewRagService;
    }

    @Override
    protected String getRolePrompt() {
        return "You are a merchant operations consultant. Provide clear and executable improvement suggestions.";
    }

    @Override
    protected String buildUserMessage(MerchantChatDTO dto, AiMerchantSession session) {
        List<ReviewVO> badReviews = reviewRagService.getReviewsByScore(session.getShopId(), 1, 3);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Low Score Reviews (1-3) ===\n");
        if (badReviews.isEmpty()) {
            sb.append("- no low-score review samples\n");
        }
        else {
            badReviews.forEach(r -> sb.append("- ").append(r.getContent()).append("\n"));
        }
        sb.append("\nUser question: ").append(dto.getMessage());
        return sb.toString();
    }
}
