package com.smartLive.search.strategy.factory;

import com.smartLive.search.strategy.DefaultEsSyncStrategy;
import com.smartLive.search.strategy.EsSyncStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate EsSyncStrategy.
 */
@Component
public class EsSyncStrategyFactory {

    private final Map<Integer, EsSyncStrategy> strategyMap = new HashMap<>();
    private final EsSyncStrategy defaultStrategy;

    @Autowired
    public EsSyncStrategyFactory(List<EsSyncStrategy> strategies, DefaultEsSyncStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (EsSyncStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public EsSyncStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
