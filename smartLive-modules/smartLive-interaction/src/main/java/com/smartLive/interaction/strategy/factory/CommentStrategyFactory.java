package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.comment.CommentStrategy;
import com.smartLive.interaction.strategy.impl.DefaultCommentStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate CommentStrategy.
 */
@Component
public class CommentStrategyFactory {

    private final Map<Integer, CommentStrategy> strategyMap = new HashMap<>();
    private final CommentStrategy defaultStrategy;

    @Autowired
    public CommentStrategyFactory(List<CommentStrategy> strategies, DefaultCommentStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (CommentStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public CommentStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
