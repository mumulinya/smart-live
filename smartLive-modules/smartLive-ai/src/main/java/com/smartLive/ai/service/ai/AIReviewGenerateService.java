package com.smartLive.ai.service.ai;

import com.smartLive.ai.entity.request.AIGenerateRequest;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.interaction.api.DTO.ReviewDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI 评价生成服务。
 */
@Slf4j
@Service
public class AIReviewGenerateService {

    private final ChatClient chatClient;
    private final IReviewRagService reviewRagService;
    private final RemoteReviewService remoteReviewService;

    public AIReviewGenerateService(
            @Qualifier("generalChatClient") ChatClient chatClient,
            IReviewRagService reviewRagService,
            RemoteReviewService remoteReviewService
    ) {
        this.chatClient = chatClient;
        this.reviewRagService = reviewRagService;
        this.remoteReviewService = remoteReviewService;
    }

    public void aiCreateReview(List<AIGenerateRequest> list) {
        List<ReviewDTO> reviews = new ArrayList<>();
        list.forEach(request -> {
            request.getSourceIds().forEach(sourceId -> {
                ReviewDTO queryCond = new ReviewDTO();
                queryCond.setSourceType(Integer.valueOf(request.getSourceType()));
                queryCond.setSourceId(sourceId);

                ReviewDTO review = createReview(queryCond);
                if (review != null) {
                    reviews.add(review);
                }
            });
        });

        if (reviews.isEmpty()) {
            return;
        }

        log.info("Will save generated reviews, size={}", reviews.size());
        remoteReviewService.saveAiCreateReview(reviews);
    }

    private ReviewDTO createReview(ReviewDTO queryDTO) {
        List<ReviewDTO> reviews = reviewRagService.getReviews(queryDTO, null);
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }

        String prompt = buildSummaryPrompt(reviews);
        String context = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setSourceType(queryDTO.getSourceType());
        reviewDTO.setSourceId(queryDTO.getSourceId());
        reviewDTO.setContent(context);
        reviewDTO.setScore(5);
        reviewDTO.setCreateTime(new Date());
        reviewDTO.setStatus(0);
        reviewDTO.setUserId(99999L);
        reviewDTO.setIsAIGenerated(true);
        return reviewDTO;
    }

    private String buildSummaryPrompt(List<ReviewDTO> reviews) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("请用一段话总结以下用户评价的核心内容，要求：\n");
        promptBuilder.append("1. 概括主要评价倾向（正面/负面/中性）\n");
        promptBuilder.append("2. 提炼用户最关注的2-3个方面\n");
        promptBuilder.append("3. 语言简洁明了，控制在100字以内\n\n");

        promptBuilder.append("用户评价：\n");

        if (reviews.isEmpty()) {
            promptBuilder.append("暂无评价数据");
        } else {
            for (int i = 0; i < reviews.size(); i++) {
                ReviewDTO review = reviews.get(i);
                promptBuilder.append(i + 1).append(". ");

                if (review.getContent() != null) {
                    promptBuilder.append(review.getContent());
                }

                if (review.getScore() != null) {
                    promptBuilder.append(" [").append(review.getScore()).append("星]");
                }

                promptBuilder.append("\n");
            }
        }

        promptBuilder.append("\n请用一段话总结：");
        return promptBuilder.toString();
    }
}
