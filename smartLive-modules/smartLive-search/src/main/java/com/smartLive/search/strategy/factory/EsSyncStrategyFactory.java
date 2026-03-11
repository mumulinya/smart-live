package com.smartLive.search.strategy.factory;

import com.smartLive.search.strategy.esSync.DefaultEsSyncStrategy;
import com.smartLive.search.strategy.esSync.EsSyncStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ES 同步策略工厂
 * 实现策略模式的自动化注册与按业务类型分发。
 * 在 Spring 容器启动时自动扫描所有 EsSyncStrategy 实现类并维护在 Map 中。
 */
@Component
public class EsSyncStrategyFactory {

    /** 策略路由表：key 为业务类型 code, value 为对应的同步处理器 */
    private final Map<Integer, EsSyncStrategy> strategyMap = new HashMap<>();
    
    /** 缺省策略：当未匹配到具体策略时使用，避免 NPE */
    private final EsSyncStrategy defaultStrategy;

    @Autowired
    public EsSyncStrategyFactory(List<EsSyncStrategy> strategies, DefaultEsSyncStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        // 自动注册所有策略实现
        for (EsSyncStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    /**
     * 根据业务类型换取同步处理器
     * 
     * @param type 业务类型代码 (如 1-博客, 2-商品)
     * @return 匹配到的策略实现或默认策略
     */
    public EsSyncStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
