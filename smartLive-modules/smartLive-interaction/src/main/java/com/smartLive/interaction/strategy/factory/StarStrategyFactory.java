package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.impl.DefaultStarStrategy;
import com.smartLive.interaction.strategy.star.StarStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate StarStrategy.
 */
@Component
public class StarStrategyFactory {

    private final Map<Integer, StarStrategy> strategyMap = new HashMap<>();
    private final StarStrategy defaultStrategy;

    @Autowired
    public StarStrategyFactory(List<StarStrategy> strategies, DefaultStarStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (StarStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public StarStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
