package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.hotrank.HotRankStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory to retrieve the appropriate HotRankStrategy.
 */
@Component
public class HotRankStrategyFactory {

    private final Map<Integer, HotRankStrategy> strategyMap = new HashMap<>();

    @Autowired
    public HotRankStrategyFactory(List<HotRankStrategy> strategies) {
        for (HotRankStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public HotRankStrategy getStrategy(Integer type) {
        return strategyMap.get(type);
    }
}
