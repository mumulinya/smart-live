package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ReviewVO;

import java.util.List;

public interface IReviewRagService {

    /**
     * 根据来源类型和ID从向量库获取评价并返回总结上下文
     *
     * @param sourceType  来源类型：1=店铺 2=文章 3=团购
     * @param sourceId    来源ID
     * @param minScore    最低评分过滤，null表示不限制
     * @param maxScore    最高评分过滤，null表示不限制
     * @param userMessage 用户原始问题
     * @return 给AI总结用的评价上下文
     */
    String getReviewSummary(Integer sourceType, Long sourceId,
                            Integer minScore, Integer maxScore, String userMessage);

}