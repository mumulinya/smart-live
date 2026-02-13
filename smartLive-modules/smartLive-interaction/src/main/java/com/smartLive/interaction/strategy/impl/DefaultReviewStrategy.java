package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.strategy.review.ReviewStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default ReviewStrategy.
 */
@Component
public class DefaultReviewStrategy implements ReviewStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public void transReviewCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // no-op
    }
}
