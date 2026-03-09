package com.smartLive.ai.tools;

import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.rag.IReviewRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewTools {

    public final IReviewRagService reviewRagService;

    /**
     * 根据条件获取评价列表（简化版 - AI工具使用）
     * @param sourceType 评价来源类型
     * @param sourceName 评价来源名称
     * @param sourceId 评价来源ID
     * @param userMessage 用户原始消息，用于RAG语义搜索
     * @return 评价列表
     */
    @Tool(name = "getReviews", description = "根据条件获取评价列表")
    public List<ReviewVO> getReviews(
            @ToolParam(description = "评价来源类型: 2=店铺 4=团购")
            int sourceType,
            @ToolParam(description = "评价来源名称")
            String sourceName,
            @ToolParam(description = "评价来源ID")
            Long sourceId,
            @ToolParam(description = "用户原始消息，用于RAG查询，例如：'味道好的评价'")
            String userMessage
    ) {
        log.info("获取评价列表，参数：sourceType={}, sourceId={}, sourceName={}, userMessage={}",
                sourceType, sourceId, sourceName, userMessage);

        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setSourceType(sourceType);
        reviewVO.setSourceId(sourceId);
        reviewVO.setSourceName(sourceName);
        reviewVO.setStatus(0);  // 只查询正常状态的评价

        List<ReviewVO> reviewList = reviewRagService.getReviews(reviewVO, userMessage);

        log.info("返回评价数量：{}", reviewList.size());
        return reviewList;
    }

    /**
     * 获取高分评价（推荐版本）
     * @param sourceType 评价来源类型
     * @param sourceId 评价来源ID
     * @param minScore 最低评分
     * @param userMessage 用户消息，用于RAG查询
     * @return 高分评价列表
     */
    @Tool(name = "getHighRatingReviews", description = "获取高分评价列表")
    public List<ReviewVO> getHighRatingReviews(
            @ToolParam(description = "评价来源类型")
            int sourceType,
            @ToolParam(description = "评价来源ID")
            Long sourceId,
            @ToolParam(description = "最低评分，默认4分")
            Integer minScore,
            @ToolParam(description = "用户消息，用于RAG语义搜索")
            String userMessage
    ) {
        log.info("获取高分评价，sourceType={}, sourceId={}, minScore={}",
                sourceType, sourceId, minScore);

        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setSourceType(sourceType);
        reviewVO.setSourceId(sourceId);
        reviewVO.setStatus(0);
        reviewVO.setMinScore(minScore != null ? minScore : 4);  // 默认4分以上

        List<ReviewVO> reviewList = reviewRagService.getReviewsByScore(reviewVO, userMessage);

        log.info("返回高分评价数量：{}", reviewList.size());
        return reviewList;
    }

    /**
     * 获取热门评价（点赞多的）
     * @param sourceType 评价来源类型
     * @param sourceId 评价来源ID
     * @param userMessage 用户消息
     * @return 热门评价列表
     */
    @Tool(name = "getPopularReviews", description = "获取热门评价列表（点赞数多）")
    public List<ReviewVO> getPopularReviews(
            @ToolParam(description = "评价来源类型")
            int sourceType,
            @ToolParam(description = "评价来源ID")
            Long sourceId,
            @ToolParam(description = "用户消息，用于RAG查询")
            String userMessage
    ) {
        log.info("获取热门评价，sourceType={}, sourceId={}", sourceType, sourceId);

        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setSourceType(sourceType);
        reviewVO.setSourceId(sourceId);
        reviewVO.setStatus(0);

        List<ReviewVO> reviewList = reviewRagService.getReviewsSortByPopularity(reviewVO, userMessage);

        log.info("返回热门评价数量：{}", reviewList.size());
        return reviewList;
    }

    /**
     * 搜索评价（纯语义搜索，不限制来源）
     * @param userMessage 用户搜索消息
     * @param limit 返回数量限制
     * @return 评价列表
     */
    @Tool(name = "searchReviews", description = "搜索评价（基于语义理解）")
    public List<ReviewVO> searchReviews(
            @ToolParam(description = "搜索关键词或描述，例如：'装修很温馨'")
            String userMessage,
            @ToolParam(description = "返回数量，默认10条")
            Integer limit
    ) {
        log.info("搜索评价，userMessage={}, limit={}", userMessage, limit);

        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setStatus(0);
        reviewVO.setLimit(limit != null ? limit : 10);

        List<ReviewVO> reviewList = reviewRagService.searchReviews(reviewVO, userMessage);

        log.info("搜索返回评价数量：{}", reviewList.size());
        return reviewList;
    }
}