package com.smartLive.ai.strategy.merchant.impl;

import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("analysisStrategy")
public class AnalysisAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;
    private final RemoteOrderService remoteOrderService;
    private final RemoteShopService remoteShopService;

    public AnalysisAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                              IAiMerchantSessionService merchantSessionService,
                              IAiMerchantMessageService merchantMessageService,
                              MerchantMessageChatMemoryManager memoryManager,
                              IReviewRagService reviewRagService,
                              RemoteOrderService remoteOrderService,
                              RemoteShopService remoteShopService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager);
        this.reviewRagService = reviewRagService;
        this.remoteOrderService = remoteOrderService;
        this.remoteShopService = remoteShopService;
    }

    @Override
    protected String getRolePrompt() {
        return "You are a restaurant operations data analyst. Provide concise insights and actionable suggestions.";
    }

    @Override
    protected String buildUserMessage(MerchantChatDTO dto, AiMerchantSession session) {
        Integer weekOrders = remoteOrderService.countWeekOrders(session.getShopId());
        if (weekOrders == null) {
            weekOrders = 0;
        }

        double avgScore = 0D;
        ShopDTO shop = remoteShopService.getShopById(session.getShopId());
        if (shop != null && shop.getScore() != null) {
            avgScore = shop.getScore() / 10.0;
        }

        List<ReviewVO> reviews = reviewRagService.searchReviews(dto.getMessage(), session.getShopId());

        StringBuilder sb = new StringBuilder();
        sb.append("=== Shop Stats ===\n");
        sb.append("Verified orders in last 7 days: ").append(weekOrders).append('\n');
        sb.append("Average score: ").append(String.format("%.1f", avgScore)).append("\n\n");
        sb.append("=== Related Reviews ===\n");
        if (reviews.isEmpty()) {
            sb.append("- no review samples\n");
        }
        else {
            reviews.forEach(r -> sb.append("- ")
                    .append(r.getContent())
                    .append(" (score: ")
                    .append(r.getScore() == null ? "unknown" : r.getScore())
                    .append(")\n"));
        }
        sb.append("\nUser question: ").append(dto.getMessage());
        return sb.toString();
    }
}
