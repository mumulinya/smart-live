package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.strategy.follow.FollowStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default FollowStrategy.
 */
@Component
public class DefaultFollowStrategy implements FollowStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public Integer getStarCount(Long sourceId) {
        return 0;
    }
}
