package com.smartLive.interaction.strategy.like;

import java.util.Map;

/**
 * 点赞行为策略接口
 * 职责：处理点赞数据的持久化、同步等写操作
 */
public interface LikeStrategy {

    /**
     * 获取策略类型 (对应 ResourceTypeEnum 的 code)
     */
    String getType();

    /**
     * 批量同步点赞数到数据库
     * @param updateMap key: 业务ID, value: 最新点赞数
     */
    void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap);
}