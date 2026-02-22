package com.smartLive.interaction.service;

/**
 * 数据同步服务
 */
public interface ISyncDataService
{
    /**
     * 主入口：并发触发所有同步任务
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
}
