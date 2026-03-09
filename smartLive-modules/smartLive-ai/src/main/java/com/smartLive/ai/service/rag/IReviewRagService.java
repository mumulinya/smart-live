package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ReviewVO;

import java.util.List;

public interface IReviewRagService {

    /**
     * 基础RAG查询：根据条件和用户消息获取评价列表
     * @param reviewVO 查询条件
     * @param userMessage 用户原始消息，用于语义搜索
     * @return 评价列表
     */
    List<ReviewVO> getReviews(ReviewVO reviewVO, String userMessage);

    /**
     * 根据评分获取评价（高分评价）
     * @param reviewVO 包含minScore的查询条件
     * @param userMessage 用户消息
     * @return 高分评价列表
     */
    List<ReviewVO> getReviewsByScore(ReviewVO reviewVO, String userMessage);

    /**
     * 按热度排序获取评价（点赞数、回复数）
     * @param reviewVO 查询条件
     * @param userMessage 用户消息
     * @return 热门评价列表，按liked+replyCount排序
     */
    List<ReviewVO> getReviewsSortByPopularity(ReviewVO reviewVO, String userMessage);

    /**
     * 纯语义搜索（不限制来源）
     * @param reviewVO 包含limit的查询条件
     * @param userMessage 搜索关键词
     * @return 匹配的评价列表
     */
    List<ReviewVO> searchReviews(ReviewVO reviewVO, String userMessage);
}