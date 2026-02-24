package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.strategy.hotrank.HotRankStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 抽象的热榜策略基类，提供公共的热度常数、时间衰减算法及通用的 Redis 工具集。
 */
@Slf4j
public abstract class AbstractHotRankStrategy implements HotRankStrategy {

    @Autowired
    protected RedisService redisService;

    @Autowired
    protected ResourceStrategyFactory resourceStrategyFactory;

    /**
     * 根据业务类型和ID列表批量获取真实的资源实体。
     * T 是真实的 VO/DTO（如 BlogVO, ProductVO）
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> getResourceList(Integer bizType, List<Long> sourceIds) {
        if (CollUtil.isEmpty(sourceIds)) return Collections.emptyList();
        ResourceStrategy strategy = resourceStrategyFactory.getStrategy(bizType);
        if (strategy == null) return Collections.emptyList();
        return strategy.getResourceList(sourceIds);
    }

    /** 热度榜计算时，需要拉取参与重算的老数据前 N 名（防霸榜机制） */
    protected static final int HOT_RANK_MERGE_TOP_N = 50;
    /** 热度衰减算法参数：基础缓冲时间（小时） */
    protected static final double HOT_SCORE_BASE_HOURS = 2.0D;
    /** 热度衰减算法参数：重力衰减因子（值越大，老数据降分越快） */
    protected static final double HOT_SCORE_GRAVITY = 1.2D;

    /**
     * 【核心算法】应用牛顿冷却/重力时间衰减公式。
     * 随着时间流逝，分母呈指数级增长，压低总分，从而给新数据出头之日。
     *
     * @param interactionScore 综合互动分（分子）
     * @param createTimeMillis 数据发布时间戳
     * @return 最终的热度排序分
     */
    protected double applyTimeDecay(double interactionScore, Long createTimeMillis) {
        long now = System.currentTimeMillis();
        long createAt = createTimeMillis == null ? now : createTimeMillis;
        double hours = Math.max(0D, (now - createAt) / 3600000D);
        return interactionScore / Math.pow(hours + HOT_SCORE_BASE_HOURS, HOT_SCORE_GRAVITY);
    }

    /**
     * 安全的 Integer 转换，防止 NPE。
     */
    protected int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * 从 ZSet 排行榜中拉取当前排名前 N 的 ID 集合。
     */
    protected Set<Long> getTopIds(String hotRankKey, int topN) {
        Set<Object> raw = redisService.getCacheZSetReverseRange(hotRankKey, 0, topN - 1L);
        return toLongSet(raw);
    }

    /**
     * 将 Redis 取出的 Object 集合安全转换为 Long 类型的 Set。
     */
    protected Set<Long> toLongSet(Set<Object> rawSet) {
        if (CollUtil.isEmpty(rawSet)) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>(rawSet.size());
        for (Object raw : rawSet) {
            if (raw == null) {
                continue;
            }
            try {
                result.add(Long.valueOf(raw.toString()));
            } catch (Exception e) {
                log.warn("redis set 中存在非法 id: {}", raw);
            }
        }
        return result;
    }
}
