package com.smartLive.interaction.service;

/**
 * 互动数据同步服务
 */
public interface ISyncDataService {

    /**
     * 触发全部同步任务
     */
    void syncAllData();

    /**
     * 同步点赞计数
     */
    void syncLikeData();

    /**
     * 同步评论计数
     */
    void syncCommentData();

    /**
     * 同步收藏计数
     */
    void syncStarData();

    /**
     * 同步评价计数
     */
    void syncReviewData();

    /**
     * 重算评论/评价热度榜
     */
    void calcHotRankData();
}
