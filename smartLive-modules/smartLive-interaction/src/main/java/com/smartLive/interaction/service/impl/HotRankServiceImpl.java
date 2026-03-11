package com.smartLive.interaction.service.impl;

import com.smartLive.interaction.service.IHotRankService;
import com.smartLive.interaction.strategy.factory.HotRankStrategyFactory;
import com.smartLive.interaction.strategy.hotrank.HotRankStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 热度榜单业务实现类
 * 负责调度各类热度算分策略，实现排行榜的定时刷新与全量重建。
 */
@Service
@Slf4j
public class HotRankServiceImpl implements IHotRankService {

    @Autowired
    private HotRankStrategyFactory hotRankStrategyFactory;

    /**
     * 按业务类型重算热度榜单数据（增量/定时任务调用）
     * 从工厂获取对应的策略，并触发 calculateAndRefreshRank
     * @param bizTypeCode 业务类型编码 (GlobalBizTypeEnum)
     */
    @Override
    public void calcHotRankDataByBizType(Integer bizTypeCode) {
        if (bizTypeCode == null) {
            log.warn("热榜算分失败：业务类型为空");
            return;
        }
        
        // 从工厂根据具体业务编码（如 BLOG, SHOP）获取对应的处理策略
        HotRankStrategy strategy = hotRankStrategyFactory.getStrategy(bizTypeCode);
        if (strategy == null) {
            log.warn("未找到对应业务类型[{}]的热榜算分策略", bizTypeCode);
            return;
        }

        long start = System.currentTimeMillis();
        log.info("开始执行[{}]类型的热度榜单重算", strategy.getClass().getSimpleName());
        
        // 交由具体策略刷新排名
        try {
            strategy.calculateAndRefreshRank();
        } catch (Exception e) {
            log.error("[{}]类型热度重算异常", strategy.getClass().getSimpleName(), e);
        }
        
        log.info("[{}]类型的热度榜单重算结束，耗时:{}ms", strategy.getClass().getSimpleName(), System.currentTimeMillis() - start);
    }

    /**
     * 按业务类型执行全量热度榜重建（凌晨定时任务调用）
     * 适用于修正长期累积的评分偏差或同步全量互动统计。
     * @param bizTypeCode 业务类型编码 (GlobalBizTypeEnum)
     */
    @Override
    public void fullRebuildHotRankByBizType(Integer bizTypeCode) {
        if (bizTypeCode == null) {
            log.warn("全量重建热榜失败：业务类型为空");
            return;
        }

        HotRankStrategy strategy = hotRankStrategyFactory.getStrategy(bizTypeCode);
        if (strategy == null) {
            log.warn("未找到对应业务类型[{}]的热榜策略", bizTypeCode);
            return;
        }

        long start = System.currentTimeMillis();
        log.info("开始执行[{}]类型的热度榜单全量重建", strategy.getClass().getSimpleName());

        try {
            strategy.fullRebuildRank();
        } catch (Exception e) {
            log.error("[{}]类型热度全量重建异常", strategy.getClass().getSimpleName(), e);
        }

        log.info("[{}]类型的热度榜单全量重建结束，耗时:{}ms", strategy.getClass().getSimpleName(), System.currentTimeMillis() - start);
    }
}
