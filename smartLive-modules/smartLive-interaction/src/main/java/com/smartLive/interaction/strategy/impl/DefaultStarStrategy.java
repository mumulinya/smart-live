package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.strategy.star.StarStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default StarStrategy.
 */
@Component
public class DefaultStarStrategy implements StarStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public void transStarCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // no-op
    }

    @Override
    public Integer getStarCount(Long sourceId) {
        return 0;
    }
}
