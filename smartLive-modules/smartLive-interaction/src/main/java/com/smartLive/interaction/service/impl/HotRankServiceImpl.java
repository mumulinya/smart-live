package com.smartLive.interaction.service.impl;

import com.smartLive.interaction.service.IHotRankService;
import com.smartLive.interaction.strategy.factory.HotRankStrategyFactory;
import com.smartLive.interaction.strategy.hotrank.HotRankStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HotRankServiceImpl implements IHotRankService {

    @Autowired
    private HotRankStrategyFactory hotRankStrategyFactory;

    @Override
    public void calcHotRankDataByBizType(Integer bizTypeCode) {
        if (bizTypeCode == null) {
            log.warn("热榜算分失败：业务类型为空");
            return;
        }
        
        // 从工厂根据具体业务编码（如 BLOG, SHOP）获取对应的处理策略
        HotRankStrategy strategy = hotRankStrategyFactory.getStrategy(bizTypeCode);
        if (strategy == null) {
            log.warn("未找到对应业务类型[{}]的热榜算分策略", bizTypeCode);
            return;
        }

        long start = System.currentTimeMillis();
        log.info("开始执行[{}]类型的热度榜单重算", strategy.getClass().getSimpleName());
        
        // 交由具体策略刷新排名
        try {
            strategy.calculateAndRefreshRank();
        } catch (Exception e) {
            log.error("[{}]类型热度重算异常", strategy.getClass().getSimpleName(), e);
        }
        
        log.info("[{}]类型的热度榜单重算结束，耗时:{}ms", strategy.getClass().getSimpleName(), System.currentTimeMillis() - start);
    }
}
