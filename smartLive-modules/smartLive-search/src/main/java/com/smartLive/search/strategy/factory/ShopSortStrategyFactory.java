package com.smartLive.search.strategy.factory;

import com.smartLive.search.strategy.shopSort.HotShopSortStrategy;
import com.smartLive.search.strategy.shopSort.ShopSortStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ES 店铺搜索排序策略工厂
 */
@Component
public class ShopSortStrategyFactory {

    private final Map<String, ShopSortStrategy> strategyMap = new HashMap<>();
    private final ShopSortStrategy defaultStrategy;

    @Autowired
    public ShopSortStrategyFactory(List<ShopSortStrategy> strategies, HotShopSortStrategy defaultStrategy) {
        // 1. 指定默认策略为 HotShopSortStrategy (智能热门推荐)
        this.defaultStrategy = defaultStrategy;
        
        // 2. 自动遍历所有实现了 ShopSortStrategy 接口的 Bean，装入 Map 中
        for (ShopSortStrategy strategy : strategies) {
            strategyMap.put(strategy.getSortType(), strategy);
        }
    }

    /**
     * 获取对应的排序策略，如果找不到对应类型，则返回默认的智能热度策略
     */
    public ShopSortStrategy getStrategy(String type) {
        if (type == null || type.trim().isEmpty()) {
            return defaultStrategy;
        }
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}