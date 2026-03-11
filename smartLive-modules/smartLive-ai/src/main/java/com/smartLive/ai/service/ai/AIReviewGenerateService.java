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
 * AI 评价生成服务
 * 负责批量生成 AI 探店评价，并调用远程服务进行保存
 *
 * @author smartLive
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

    /**
     * 批量创建 AI 评价
     * 遍历请求列表，为每个来源（如店铺、商品）生成 AI 评价并统一保存
     *
     * @param list 包含来源类型和来源 ID 列表的请求对象
     */
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

        log.info("准备保存生成的 AI 评价，数量: {}", reviews.size());
        remoteReviewService.saveAiCreateReview(reviews);
    }

    /**
     * 为单个来源生成 AI 评价
     * 调用大模型根据历史评价（RAG 逻辑，当前示例中暂留空）生成总结性的新评价
     *
     * @param queryDTO 包含来源类型和 ID 的查询对象
     * @return 生成好的 ReviewDTO 对象，若无历史评价则返回 null
     */
    private ReviewDTO createReview(ReviewDTO queryDTO) {
//        List<ReviewDTO> reviews = reviewRagService.getReviews(null, null);
        List<ReviewDTO> reviews=new ArrayList<>();
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
        reviewDTO.setUserId(99999L); // 默认 AI 专用用户 ID
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
