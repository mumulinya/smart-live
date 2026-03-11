package com.smartLive.interaction.job;

import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.service.IHotRankService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * 凌晨全量重建定时任务处理器
 * 建议 CRON: 0 0 3 * * ?（每天凌晨3点执行）
 * 包含所有业务类型的热榜全量重建任务：博客、店铺、商品、评价、评论
 */
@Component
@Slf4j
public class FullRebuildJobHandler {

    @Autowired
    private IHotRankService hotRankService;

    @Autowired
    private ExecutorService executorService;

    /**
     * 一键全量重建所有类型的热榜
     */
    @XxlJob("allFullRebuildRankJob")
    public ReturnT<String> allFullRebuildRankJob() {
        log.info("触发 xxl-job: 所有热榜全量重建任务 allFullRebuildRankJob");
        executorService.execute(() -> {
            for (GlobalBizTypeEnum bizType : GlobalBizTypeEnum.values()) {
                // 跳过 USER 类型（用户没有热榜）
                if (bizType == GlobalBizTypeEnum.USER) {
                    continue;
                }
                try {
                    log.info("开始全量重建[{}]热榜", bizType.getDesc());
                    hotRankService.fullRebuildHotRankByBizType(bizType.getCode());
                    log.info("[{}]热榜全量重建完成", bizType.getDesc());
                } catch (Exception e) {
                    log.error("[{}]热榜全量重建异常", bizType.getDesc(), e);
                }
            }
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 凌晨全量重建博客热榜
     */
    @XxlJob("blogFullRebuildRankJob")
    public ReturnT<String> blogFullRebuildRankJob() {
        log.info("触发 xxl-job: 博客热榜全量重建任务 blogFullRebuildRankJob");
        executorService.execute(() -> {
            hotRankService.fullRebuildHotRankByBizType(GlobalBizTypeEnum.BLOG.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 凌晨全量重建店铺热榜
     */
    @XxlJob("shopFullRebuildRankJob")
    public ReturnT<String> shopFullRebuildRankJob() {
        log.info("触发 xxl-job: 店铺热榜全量重建任务 shopFullRebuildRankJob");
        executorService.execute(() -> {
            hotRankService.fullRebuildHotRankByBizType(GlobalBizTypeEnum.SHOP.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 凌晨全量重建商品热榜
     */
    @XxlJob("productFullRebuildRankJob")
    public ReturnT<String> productFullRebuildRankJob() {
        log.info("触发 xxl-job: 商品热榜全量重建任务 productFullRebuildRankJob");
        executorService.execute(() -> {
            hotRankService.fullRebuildHotRankByBizType(GlobalBizTypeEnum.PRODUCT.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 凌晨全量重建评价热榜
     */
    @XxlJob("reviewFullRebuildRankJob")
    public ReturnT<String> reviewFullRebuildRankJob() {
        log.info("触发 xxl-job: 评价热榜全量重建任务 reviewFullRebuildRankJob");
        executorService.execute(() -> {
            hotRankService.fullRebuildHotRankByBizType(GlobalBizTypeEnum.REVIEW.getCode());
        });
        return ReturnT.SUCCESS;
    }

    /**
     * 凌晨全量重建评论热榜
     */
    @XxlJob("commentFullRebuildRankJob")
    public ReturnT<String> commentFullRebuildRankJob() {
        log.info("触发 xxl-job: 评论热榜全量重建任务 commentFullRebuildRankJob");
        executorService.execute(() -> {
            hotRankService.fullRebuildHotRankByBizType(GlobalBizTypeEnum.COMMENT.getCode());
        });
        return ReturnT.SUCCESS;
    }
}
