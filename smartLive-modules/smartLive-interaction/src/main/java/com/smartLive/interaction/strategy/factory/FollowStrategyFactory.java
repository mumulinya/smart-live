package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.follow.FollowStrategy;
import com.smartLive.interaction.strategy.impl.DefaultFollowStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate FollowStrategy.
 */
@Component
public class FollowStrategyFactory {

    private final Map<Integer, FollowStrategy> strategyMap = new HashMap<>();
    private final FollowStrategy defaultStrategy;

    @Autowired
    public FollowStrategyFactory(List<FollowStrategy> strategies, DefaultFollowStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (FollowStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public FollowStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
