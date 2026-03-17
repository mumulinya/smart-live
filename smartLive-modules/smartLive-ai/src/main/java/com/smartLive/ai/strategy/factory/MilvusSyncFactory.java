package com.smartLive.ai.strategy.factory;

import com.smartLive.ai.strategy.milvus.DefaultMilvusSyncStrategy;
import com.smartLive.ai.strategy.milvus.MilvusSyncStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用于获取合适 Milvus 同步策略的工厂类。
 */
@Component
public class MilvusSyncFactory {

    private final Map<Integer, MilvusSyncStrategy> strategyMap = new HashMap<>();
    private final MilvusSyncStrategy defaultStrategy;

    /**
     * 构造Milvus同步工厂。
     */
    @Autowired
    public MilvusSyncFactory(List<MilvusSyncStrategy> strategies, DefaultMilvusSyncStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (MilvusSyncStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    /**
     * 获取策略。
     */
    public MilvusSyncStrategy getStrategy(Integer type) {
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}
