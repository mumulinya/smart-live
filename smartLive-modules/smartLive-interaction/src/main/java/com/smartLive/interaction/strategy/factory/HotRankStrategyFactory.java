package com.smartLive.interaction.strategy.factory;

import com.smartLive.interaction.strategy.hotrank.HotRankStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热榜评分策略工厂
 * 用于根据业务类型动态获取对应的热榜算分策略实现。
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

    /**
     * 根据业务类型编码获取热榜策略
     * @param type 业务类型编码 (GlobalBizTypeEnum)
     * @return 对应的策略实现，未找到则返回 null
     */
    public HotRankStrategy getStrategy(Integer type) {
        return strategyMap.get(type);
    }
}
