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
 * 店铺排序策略工厂
 * 利用 Spring 的依赖注入特性，自动管理所有 ShopSortStrategy 实现。
 * 支持根据前端传入的 type 动态路由到不同的 ES 排序构建逻辑。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component
public class ShopSortStrategyFactory {

    /** 策略注册表：key 为排序标识，value 为策略 Bean */
    private final Map<String, ShopSortStrategy> strategyMap = new HashMap<>();
    
    /** 兜底策略：当未指定或未找到对应策略时使用的智能推荐策略 */
    private final ShopSortStrategy defaultStrategy;

    @Autowired
    public ShopSortStrategyFactory(List<ShopSortStrategy> strategies, HotShopSortStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        
        // 自动注册容器中所有的排序 Bean
        for (ShopSortStrategy strategy : strategies) {
            strategyMap.put(strategy.getSortType(), strategy);
        }
    }

    /**
     * 根据排序类型获取对应处理器
     * 
     * @param type 排序类型 (hot, distance, price, score)
     * @return 匹配的策略实现，默认返回 HotShopSortStrategy
     */
    public ShopSortStrategy getStrategy(String type) {
        if (type == null || type.trim().isEmpty()) {
            return defaultStrategy;
        }
        return Optional.ofNullable(strategyMap.get(type)).orElse(defaultStrategy);
    }
}