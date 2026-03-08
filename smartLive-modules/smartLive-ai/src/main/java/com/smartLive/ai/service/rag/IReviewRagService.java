package com.smartLive.ai.service.rag;

import com.smartLive.interaction.api.DTO.ReviewDTO;

import java.util.List;

public interface IReviewRagService {
    /**
     * 获取评价列表
     *
     * @param reviewDTO 查询条件
     * @param userMessage 用户原始消息
     * @return 评价列表
     */
    List<ReviewDTO> getReviews(ReviewDTO reviewDTO, String userMessage);
}
