package com.smartLive.ai.strategy.merchant.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.MerchantAiSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import com.smartLive.ai.service.merchant.IMerchantAiSessionService;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.shop.api.DTO.ShopAnalysisDTO;
import com.smartLive.shop.api.RemoteShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component("analysisStrategy")
@Slf4j
public class AnalysisAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;

    public AnalysisAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                              IMerchantAiSessionService merchantSessionService,
                              IMerchantAiMessageService merchantMessageService,
                              MerchantMessageChatMemoryManager memoryManager,
                              RemoteShopService remoteShopService,
                              IShopRagService shopRagService,
                              ObjectMapper objectMapper,
                              IReviewRagService reviewRagService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager,
                remoteShopService, shopRagService, objectMapper);
        this.reviewRagService = reviewRagService;
    }

    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, MerchantAiSession session) {
        ShopAnalysisDTO analysis = remoteShopService.getShopAnalysis(session.getShopId(), dto.getTimeRange());
        log.info("查询的周期为:{},店铺的 analysis: {}",dto.getTimeRange(), analysis);
        List<ReviewVO> badReviews = reviewRagService.getReviewsByScore(session.getShopId(), 1, 3);
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());

        return """
                SCENE: BUSINESS_ANALYSIS

                You are generating a business analysis report for the merchant.

                Shop:
                - name: %s
                - type: %s
                - range: %s

                Shop metrics:
                - totalOrders: %s
                - totalRevenue: %s
                - avgScore: %s
                - badReviewCount: %s

                Low-score review samples from RAG:
                %s

                Extra instruction:
                %s

                Requirements:
                1. Output language: Simplified Chinese.
                2. Structure the answer into three parts: current status, key problems, action plan.
                3. Combine the metrics and the bad review samples to explain the situation.
                4. When discussing problems, cite the review sample trends instead of giving generic statements.
                5. Do not invent data that is not provided.
                6. Do not output JSON.
                """.formatted(
                shopContext.getShopName(),
                shopContext.getShopType(),
                dto.getTimeRange(),
                defaultNumber(analysis.getTotalOrders()),
                defaultDecimal(analysis.getTotalRevenue()),
                defaultDecimal(analysis.getAvgScore()),
                defaultNumber(analysis.getBadReviewCount()),
                formatBadReviewSamples(badReviews),
                resolveInstruction(dto)
        );
    }

    private String formatBadReviewSamples(List<ReviewVO> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "No data";
        }
        List<String> samples = new ArrayList<>();
        for (ReviewVO review : reviews) {
            if (review == null || !StringUtils.hasText(review.getContent())) {
                continue;
            }
            samples.add("- [score=" + defaultNumber(review.getScore()) + "] " + review.getContent().trim());
            if (samples.size() >= 5) {
                break;
            }
        }
        return samples.isEmpty() ? "No data" : String.join("\n", samples);
    }
}