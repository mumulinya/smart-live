package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.strategy.like.LikeStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default LikeStrategy.
 */
@Component
public class DefaultLikeStrategy implements LikeStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // no-op
    }

    @Override
    public Integer getLikeCount(Long sourceId) {
        return 0;
    }
}
