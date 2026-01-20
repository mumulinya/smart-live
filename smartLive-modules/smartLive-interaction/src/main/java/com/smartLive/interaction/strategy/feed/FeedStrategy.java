package com.smartLive.interaction.strategy.feed;

import com.smartLive.common.core.domain.ScrollResult;

public interface FeedStrategy {
    /**
     * 策略对应的方法：分页查询动态
     */
    ScrollResult queryPage(Long userId, Long max, Integer offset);
    
    /**
     * 策略标识 (用于工厂匹配)
     * 例如: ALL, BLOG, SHOP, ITEM
     */
    Integer getType();
    /**
     * 获取动态列表 Redis Key
     */
    String getFeedKey(Long userId);
}