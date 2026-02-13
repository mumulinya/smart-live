package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.impl.DefaultReviewStrategy;
import com.smartLive.interaction.strategy.review.ReviewStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate ReviewStrategy.
 */
@Component
public class ReviewStrategyFactory {

    private final Map<Integer, ReviewStrategy> strategyMap = new HashMap<>();
    private final ReviewStrategy defaultStrategy;

    @Autowired
    public ReviewStrategyFactory(List<ReviewStrategy> strategies, DefaultReviewStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (ReviewStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public ReviewStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
