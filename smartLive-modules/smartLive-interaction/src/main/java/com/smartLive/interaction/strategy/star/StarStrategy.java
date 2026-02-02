package com.smartLive.interaction.strategy.star;

import java.util.Map;

/**
 * 收藏行为策略接口
 * 职责：处理收藏数据的持久化、同步等写操作
 */
public interface StarStrategy {

    /**
     * 获取策略类型 (对应 ResourceTypeEnum 的 code)
     */
    Integer getType();

    /**
     * 批量同步点赞数到数据库
     * @param updateMap key: 业务ID, value: 最新点赞数
     */
    void transStarCountFromRedis2DB(Map<Long, Integer> updateMap);
    /**
     * 获取收藏数
     * @param sourceId 业务ID
     * @return 收藏数
     */
    Integer getStarCount(Long sourceId);
    /**
     * 同步数据到ES
     * 使用 default 关键字提供默认空实现
     * 只有需要同步搜索的资源（如博客、店铺）才需要重写此方法
     */
    default void syncUserResource(Long userId,Long sourceId) {
    }
}