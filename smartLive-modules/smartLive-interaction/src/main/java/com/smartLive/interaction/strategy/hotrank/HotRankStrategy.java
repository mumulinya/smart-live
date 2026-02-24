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
}
