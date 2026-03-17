package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ReviewVO;

import java.util.List;

/**
 * 评价 RAG 服务接口。
 */
public interface IReviewRagService {

    /**
     * 获取评价摘要。
     */
    String getReviewSummary(Integer sourceType, Long sourceId,
                            Integer minScore, Integer maxScore, String userMessage);

    /**
     * 搜索评价。
     */
    List<ReviewVO> searchReviews(String query, Long shopId);

    /**
     * 按评分获取评价列表。
     */
    List<ReviewVO> getReviewsByScore(Long shopId, Integer minScore, Integer maxScore);

    /**
     * 按 ID 获取评价。
     */
    ReviewVO getReviewById(Long reviewId, Long shopId);
}