package com.smartLive.interaction.task;

import com.smartLive.interaction.service.ISyncDataService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * 互动同步定时任务处理器 (XXL-JOB)。
 * 职责：定时将 Redis 中的互动数据异步同步到 MySQL。
 */
@Component
@Slf4j
public class InteractionSyncXxlJob {

    @Autowired
    private ISyncDataService syncDataService;

    @Autowired
    private ExecutorService executorService;

    /**
     * 同步所有互动数据（点赞、评论、收藏、评价、关注/粉丝）到数据库。
     */
    @XxlJob("interactionSyncAllDataJob")
    public ReturnT<String> executeAllDataSync() {
        log.info("触发 XXL-JOB 定时任务：同步所有互动数据到数据库(interactionSyncAllDataJob)");
        executorService.execute(() -> {
            try {
                syncDataService.syncAllData();
                log.info("异步执行完成：同步所有互动数据成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步所有互动数据异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 仅同步点赞数据到数据库。
     */
    @XxlJob("interactionSyncLikeJob")
    public ReturnT<String> executeLikeSync() {
        log.info("触发 XXL-JOB 定时任务：同步点赞数据到数据库(interactionSyncLikeJob)");
        executorService.execute(() -> {
            try {
                syncDataService.syncLikeData();
                log.info("异步执行完成：同步点赞数据成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步点赞数据异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 仅同步评论数据到数据库。
     */
    @XxlJob("interactionSyncCommentJob")
    public ReturnT<String> executeCommentSync() {
        log.info("触发 XXL-JOB 定时任务：同步评论数据到数据库(interactionSyncCommentJob)");
        executorService.execute(() -> {
            try {
                syncDataService.syncCommentData();
                log.info("异步执行完成：同步评论数据成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步评论数据异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 仅同步收藏数据到数据库。
     */
    @XxlJob("interactionSyncStarJob")
    public ReturnT<String> executeStarSync() {
        log.info("触发 XXL-JOB 定时任务：同步收藏数据到数据库(interactionSyncStarJob)");
        executorService.execute(() -> {
            try {
                syncDataService.syncStarData();
                log.info("异步执行完成：同步收藏数据成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步收藏数据异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 仅同步评价数据到数据库。
     */
    @XxlJob("interactionSyncReviewJob")
    public ReturnT<String> executeReviewSync() {
        log.info("触发 XXL-JOB 定时任务：同步评价数据到数据库(interactionSyncReviewJob)");
        executorService.execute(() -> {
            try {
                syncDataService.syncReviewData();
                log.info("异步执行完成：同步评价数据成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步评价数据异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 仅同步关注计数到数据库。
     */
    @XxlJob("interactionSyncFollowJob")
    public ReturnT<String> executeFollowSync() {
        log.info("触发 XXL-JOB 定时任务：同步关注计数到数据库(interactionSyncFollowJob)");
        executorService.execute(() -> {
            try {
                syncDataService.syncFollowData();
                log.info("异步执行完成：同步关注计数成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步关注计数异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 仅同步粉丝计数到数据库。
     */
    @XxlJob("interactionSyncFansJob")
    public ReturnT<String> executeFansSync() {
        log.info("触发 XXL-JOB 定时任务：同步粉丝计数到数据库(interactionSyncFansJob)");
        executorService.execute(() -> {
            try {
                syncDataService.syncFansData();
                log.info("异步执行完成：同步粉丝计数成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步粉丝计数异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }
}
