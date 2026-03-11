package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.strategy.hotrank.HotRankStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 抽象的热榜策略基类。
 * 提供公共的工具方法（Redis 操作、资源批量获取、时间衰减计算等），
 * 具体的评分公式由各子类实现。
 *
 * 评分体系说明（参见《SmartLive 热度评分体系设计》文档）：
 * - 每种业务类型都有一个初始分（baseScore），避免冷启动排在最后。
 * - 热度分 = baseScore + 各互动字段 × 对应权重。
 * - 博客/评价/评论额外包含"时间衰减"项（30天线性衰减）。
 * - 店铺/商品不做时间衰减，靠销量和口碑驱动热度。
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

    /**
     * 计算时间衰减值。
     * 规则：发布30天内有加成，30天后加成为0。
     * - 发布当天：timeDecay = 30（最高加成）
     * - 发布15天：timeDecay = 15（中等加成）
     * - 超过30天：timeDecay = 0（无加成，靠互动维持热度）
     *
     * @param createTime 发布/创建时间
     * @return 时间衰减加成值（0～30）
     */
    protected double calcTimeDecay(Date createTime) {
        if (createTime == null) return 0;
        long diffMs = System.currentTimeMillis() - createTime.getTime();
        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        return Math.max(0, 30 - days);
    }

    /**
     * 对最终热度分数保留两位小数，提升 UI 展示美观度。
     */
    protected double roundScore(double score) {
        return Math.round(score * 100.0) / 100.0;
    }

    /**
     * 安全的 Integer/Number 转换，防止 NPE。
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
