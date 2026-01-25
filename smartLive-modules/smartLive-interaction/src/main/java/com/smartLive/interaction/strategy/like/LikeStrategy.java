package com.smartLive.interaction.strategy.like;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 点赞行为策略接口
 * 职责：处理点赞数据的持久化、同步等写操作
 */
public interface LikeStrategy {

    /**
     * 获取策略类型 (对应 ResourceTypeEnum 的 code)
     */
    Integer getType();

    /**
     * 批量同步点赞数到数据库
     * @param updateMap key: 业务ID, value: 最新点赞数
     */
    void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap);
    /**
     * 获取点赞数
     * @param sourceId 业务ID
     * @return 点赞数
     */
    Integer getLikeCount(Long sourceId);
}