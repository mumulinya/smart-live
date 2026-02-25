package com.smartLive.interaction.strategy.hotrank;

/**
 * 热榜计算策略接口
 * 职责：按业务类型分别处理 ZSet 排行榜的洗牌和积分重算
 */
public interface HotRankStrategy {

    /**
     * 获取策略类型 (对应 GlobalBizTypeEnum 的 code)
     */
    Integer getType();

    /**
     * 针对该业务线的一批被互动的源ID，计算热度分并刷新排行榜
     */
    void calculateAndRefreshRank();

    /**
     * 全量重建热榜（凌晨定时任务使用）
     * 从数据库获取全部数据重新计算热度分，覆盖写入 ZSet
     */
    default void fullRebuildRank() {
        throw new UnsupportedOperationException("该业务类型不支持全量重建");
    }
}
