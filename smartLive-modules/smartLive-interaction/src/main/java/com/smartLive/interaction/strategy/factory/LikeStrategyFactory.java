package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.impl.DefaultLikeStrategy;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate LikeStrategy.
 */
@Component
public class LikeStrategyFactory {

    private final Map<Integer, LikeStrategy> strategyMap = new HashMap<>();
    private final LikeStrategy defaultStrategy;

    @Autowired
    public LikeStrategyFactory(List<LikeStrategy> strategies, DefaultLikeStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (LikeStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public LikeStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
