package com.smartLive.interaction.task;

import com.smartLive.interaction.service.ISyncDataService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 互动同步任务触发入口
 */
@Component
@Slf4j
public class InteractionSyncXxlJob {

    @Autowired
    private ISyncDataService syncDataService;

    @XxlJob("interactionSyncAllDataJob")
    public void executeAllDataSync() {
        log.info("xxl-job start: interactionSyncAllDataJob");
        syncDataService.syncAllData();
    }

    @XxlJob("interactionSyncLikeJob")
    public void executeLikeSync() {
        log.info("xxl-job start: interactionSyncLikeJob");
        syncDataService.syncLikeData();
    }

    @XxlJob("interactionSyncCommentJob")
    public void executeCommentSync() {
        log.info("xxl-job start: interactionSyncCommentJob");
        syncDataService.syncCommentData();
    }

    @XxlJob("interactionSyncStarJob")
    public void executeStarSync() {
        log.info("xxl-job start: interactionSyncStarJob");
        syncDataService.syncStarData();
    }

    @XxlJob("interactionSyncReviewJob")
    public void executeReviewSync() {
        log.info("xxl-job start: interactionSyncReviewJob");
        syncDataService.syncReviewData();
    }

    @XxlJob("interactionCalcHotRankJob")
    public void executeHotRankCalc() {
        log.info("xxl-job start: interactionCalcHotRankJob");
        syncDataService.calcHotRankData();
    }
}
