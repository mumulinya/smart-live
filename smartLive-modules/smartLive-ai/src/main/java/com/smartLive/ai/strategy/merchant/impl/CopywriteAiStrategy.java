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

@Component("copywriteStrategy")
public class CopywriteAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;

    public CopywriteAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                               IAiMerchantSessionService merchantSessionService,
                               IAiMerchantMessageService merchantMessageService,
                               MerchantMessageChatMemoryManager memoryManager,
                               IReviewRagService reviewRagService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager);
        this.reviewRagService = reviewRagService;
    }

    @Override
    protected String getRolePrompt() {
        return "You are a marketing copywriter for restaurants. Write vivid and persuasive copy.";
    }

    @Override
    protected String buildUserMessage(MerchantChatDTO dto, AiMerchantSession session) {
        List<ReviewVO> reviews = reviewRagService.searchReviews(dto.getMessage(), session.getShopId());
        StringBuilder sb = new StringBuilder();
        sb.append("=== Review References ===\n");
        if (reviews.isEmpty()) {
            sb.append("- no review samples\n");
        }
        else {
            reviews.forEach(r -> sb.append("- ").append(r.getContent()).append('\n'));
        }
        sb.append("\nProduct information: ").append(dto.getMessage());
        return sb.toString();
    }
}
