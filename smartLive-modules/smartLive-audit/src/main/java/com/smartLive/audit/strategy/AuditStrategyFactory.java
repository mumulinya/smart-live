package com.smartLive.audit.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审核策略工厂
 */
@Component
public class AuditStrategyFactory {

    private final Map<Integer, AuditStrategy> strategyMap = new HashMap<>();
    private final AuditStrategy defaultStrategy;

    /**
     * 构建审核策略工厂
     *
     * @param strategies     已注册的审核策略
     * @param defaultStrategy 默认策略
     */
    @Autowired
    public AuditStrategyFactory(List<AuditStrategy> strategies, DefaultAuditStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (AuditStrategy strategy : strategies) {
            strategyMap.put(strategy.getBizType(), strategy);
        }
    }

    /**
     * 根据业务类型获取审核策略
     *
     * @param bizType 业务类型编码
     * @return 对应策略；未匹配则返回默认策略
     */
    public AuditStrategy getStrategy(Integer bizType) {
        return Optional.ofNullable(strategyMap.get(bizType)).orElse(defaultStrategy);
    }
}
