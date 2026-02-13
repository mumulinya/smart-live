package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.strategy.comment.CommentStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default CommentStrategy.
 */
@Component
public class DefaultCommentStrategy implements CommentStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public void transCommentCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // no-op
    }
}
