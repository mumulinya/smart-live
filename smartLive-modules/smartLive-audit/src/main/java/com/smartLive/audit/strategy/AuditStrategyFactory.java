package com.smartLive.audit.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate AuditStrategy.
 */
@Component
public class AuditStrategyFactory {

    private final Map<Integer, AuditStrategy> strategyMap = new HashMap<>();
    private final AuditStrategy defaultStrategy;

    @Autowired
    public AuditStrategyFactory(List<AuditStrategy> strategies, DefaultAuditStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (AuditStrategy strategy : strategies) {
            strategyMap.put(strategy.getBizType(), strategy);
        }
    }

    /**
     * Get strategy by business type.
     * @param bizType The business type ID
     * @return The specific strategy or the default strategy if not found
     */
    public AuditStrategy getStrategy(Integer bizType) {
        return Optional.ofNullable(strategyMap.get(bizType)).orElse(defaultStrategy);
    }
}
