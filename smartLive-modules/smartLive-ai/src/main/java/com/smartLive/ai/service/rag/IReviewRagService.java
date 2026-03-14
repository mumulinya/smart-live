package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ReviewVO;

import java.util.List;

public interface IReviewRagService {

    String getReviewSummary(Integer sourceType, Long sourceId,
                            Integer minScore, Integer maxScore, String userMessage);

    List<ReviewVO> searchReviews(String query, Long shopId);

    List<ReviewVO> getReviewsByScore(Long shopId, Integer minScore, Integer maxScore);

    ReviewVO getReviewById(Long reviewId, Long shopId);
}