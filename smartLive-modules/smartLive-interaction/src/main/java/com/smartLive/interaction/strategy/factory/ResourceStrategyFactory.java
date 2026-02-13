package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.impl.DefaultResourceStrategy;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate ResourceStrategy.
 */
@Component
public class ResourceStrategyFactory {

    private final Map<Integer, ResourceStrategy> strategyMap = new HashMap<>();
    private final ResourceStrategy defaultStrategy;

    @Autowired
    public ResourceStrategyFactory(List<ResourceStrategy> strategies, DefaultResourceStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (ResourceStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public ResourceStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
