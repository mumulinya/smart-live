package com.smartLive.interaction.strategy.follow;

import java.util.Map;

/**
 * 关注行为策略接口
 * 职责：处理收藏数据的持久化、同步等写操作
 */
public interface FollowStrategy {

    /**
     * 获取策略类型 (对应 ResourceTypeEnum 的 code)
     */
    Integer getType();

    /**
     * Batch sync follow count to DB.
     * key: userId, value: latest follow count
     */
    default void transFollowCountFromRedis2DB(Map<Long, Integer> updateMap) {
    }

    /**
     * Batch sync fans count to DB.
     * key: sourceId, value: latest fans count
     */
    default void transFansCountFromRedis2DB(Map<Long, Integer> updateMap) {
    }
    /**
     * 获取收藏数
     * @param sourceId 业务ID
     * @return 收藏数
     */
    Integer getStarCount(Long sourceId);

    /**
     * 从源数据表获取粉丝数（如 user_info.fans、shop.fans、product.fans）
     * @param sourceId 被关注实体 ID
     * @return 粉丝数，null 表示源表无数据
     */
    default Integer getFanCount(Long sourceId) {
        return null;
    }

    /**
     * 从源数据表获取关注数（如 user_info.followee）
     * @param userId 用户 ID
     * @return 关注数，null 表示源表无数据
     */
    default Integer getFollowCount(Long userId) {
        return null;
    }

    /**
     * 同步数据到ES
     * 使用 default 关键字提供默认空实现
     * 只有需要同步搜索的资源（如博客、店铺）才需要重写此方法
     */
    default void syncUserResource(Long userId,Long sourceId) {
    }
}
