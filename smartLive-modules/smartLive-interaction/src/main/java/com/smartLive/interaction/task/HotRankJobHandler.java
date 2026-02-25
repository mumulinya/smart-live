package com.smartLive.interaction.task;

import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.service.IHotRankService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * 热榜计算的 xxl-job 定时任务处理器
 */
@Component
@Slf4j
public class HotRankJobHandler {

    @Autowired
    private IHotRankService hotRankService;

    @Autowired
    private ExecutorService executorService;

    /**
     * 定时统计博客热榜并洗牌排行榜
     */
    @XxlJob("blogHotRankJobHandler")
    public ReturnT<String> blogHotRankJobHandler() throws Exception {
        log.info("触发 xxl-job: 博客热榜计算任务 blogHotRankJobHandler");
        executorService.execute(() -> {
            hotRankService.calcHotRankDataByBizType(GlobalBizTypeEnum.BLOG.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 定时统计店铺热榜并洗牌排行榜
     */
    @XxlJob("shopHotRankJobHandler")
    public ReturnT<String> shopHotRankJobHandler() throws Exception {
        log.info("触发 xxl-job: 店铺热榜计算任务 shopHotRankJobHandler");
        executorService.execute(() -> {
            hotRankService.calcHotRankDataByBizType(GlobalBizTypeEnum.SHOP.getCode());
        });
        return ReturnT.SUCCESS;
    }
    /**
     * 定时统计商品热榜并洗牌排行榜
     */
    @XxlJob("productHotRankJobHandler")
    public ReturnT<String> productHotRankJobHandler() throws Exception {
        log.info("触发 xxl-job: 商品热榜计算任务 productHotRankJobHandler");
        executorService.execute(() -> {
            hotRankService.calcHotRankDataByBizType(GlobalBizTypeEnum.PRODUCT.getCode());
        });
        return ReturnT.SUCCESS;
    }
    /**
     * 定时统计评价热榜并洗牌排行榜
     */
    @XxlJob("reviewHotRankJobHandler")
    public ReturnT<String> reviewHotRankJobHandler() throws Exception {
        log.info("触发 xxl-job: 评价热榜计算任务 reviewHotRankJobHandler");
        executorService.execute(() -> {
            hotRankService.calcHotRankDataByBizType(GlobalBizTypeEnum.REVIEW.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 定时统计评论热榜并洗牌排行榜 (针对评论区的二次回复)
     */
    @XxlJob("commentHotRankJobHandler")
    public ReturnT<String> commentHotRankJobHandler() throws Exception {
        log.info("触发 xxl-job: 普通评论热榜计算任务 commentHotRankJobHandler");
        executorService.execute(() -> {
            hotRankService.calcHotRankDataByBizType(GlobalBizTypeEnum.COMMENT.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 凌晨全量重建店铺热榜（建议 CRON: 0 0 3 * * ?）
     */
    @XxlJob("shopFullRebuildRankJob")
    public ReturnT<String> shopFullRebuildRankJob() throws Exception {
        log.info("触发 xxl-job: 店铺热榜全量重建任务 shopFullRebuildRankJob");
        executorService.execute(() -> {
            hotRankService.fullRebuildHotRankByBizType(GlobalBizTypeEnum.SHOP.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 凌晨全量重建博客热榜（建议 CRON: 0 0 3 * * ?）
     */
    @XxlJob("blogFullRebuildRankJob")
    public ReturnT<String> blogFullRebuildRankJob() throws Exception {
        log.info("触发 xxl-job: 博客热榜全量重建任务 blogFullRebuildRankJob");
        executorService.execute(() -> {
            hotRankService.fullRebuildHotRankByBizType(GlobalBizTypeEnum.BLOG.getCode());
        });
        return ReturnT.SUCCESS;
    }
}
