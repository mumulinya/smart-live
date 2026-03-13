package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.common.RankRedisEnum;
import com.smartLive.common.core.enums.interaction.ReviewTypeEnum;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.mapper.ReviewMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价实体全局热度计算策略
 * 
 * 核心职能：
 * 1. 针对店铺评价、商品评价进行分类热度统计。
 * 2. 算法加权侧重于“点赞（有用度）”、“评分高低”及“是否带图”。
 * 3. 遵循 30 天时间衰减规则，置顶优质且新鲜的真实评价。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Component
public class ReviewHotRankStrategyImpl extends AbstractHotRankStrategy {

    @Autowired
    protected RedisService redisService;
    @Autowired
    protected ReviewMapper reviewMapper;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.REVIEW.getCode();
    }

    /**
     * 刷新评价热度榜单
     * 逻辑：根据不同的评价类型（SHOP/PRODUCT），分别计算其 sourceId 下的评价热度排名。
     */
    @Override
    public void calculateAndRefreshRank() {
        Arrays.stream(ReviewTypeEnum.values()).forEach(type -> {
            RankRedisEnum redisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", type.getCode());
            if (redisEnum == null || redisEnum.getCalcQueueKey() == null) {
                return;
            }

            String calcKey = redisEnum.getCalcQueueKey();
            String tempKey = calcKey + ":TEMP";
            try {
                if (Boolean.FALSE.equals(redisService.hasKey(calcKey))) return;
                
                redisService.rename(calcKey, tempKey);
                Set<Object> activeSet = redisService.getCacheSet(tempKey);
                if (CollUtil.isEmpty(activeSet)) {
                    redisService.deleteObject(tempKey);
                    return;
                }

                Set<Long> activeIds = toLongSet(activeSet);
                Map<Long, Review> activeReviewMap = toReviewMap(reviewMapper.selectBatchIds(activeIds));
                if (activeReviewMap.isEmpty()) {
                    redisService.deleteObject(tempKey);
                    return;
                }

                Map<Long, Set<Long>> sourceIdToIds = new LinkedHashMap<>();
                for (Review review : activeReviewMap.values()) {
                    if (review == null || review.getId() == null || review.getSourceId() == null) continue;
                    // Integer 和 Long 可能需要兼容判断
                    if (!Objects.equals(review.getSourceType(), type.getCode())) continue;
                    sourceIdToIds.computeIfAbsent(review.getSourceId(), k -> new LinkedHashSet<>()).add(review.getId());
                }

                for (Map.Entry<Long, Set<Long>> entry : sourceIdToIds.entrySet()) {
                    Long sourceId = entry.getKey();
                    Set<Long> candidateIds = new LinkedHashSet<>(entry.getValue());
                    String hotRankKey = redisEnum.getHotRankKeyPrefix() + sourceId;

                    candidateIds.addAll(getTopIds(hotRankKey, HOT_RANK_MERGE_TOP_N));
                    
                    Map<Long, Review> candidateMap = toReviewMap(reviewMapper.selectBatchIds(candidateIds));
                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();

                    for (Long id : candidateIds) {
                        Review review = candidateMap.get(id);
                        if (review == null || !ObjectUtil.equals(review.getSourceId(), sourceId)) {
                            redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                            continue;
                        }
                        if (review.getAuditStatus() != null && review.getAuditStatus() == 2) {
                            redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                            if (redisEnum.getNewRankKeyPrefix() != null) {
                                redisService.removeCacheZSetObject(redisEnum.getNewRankKeyPrefix() + sourceId, String.valueOf(id));
                            }
                            continue;
                        }
                        double score = calcReviewHotScore(type, review);
                        tuples.add(new DefaultTypedTuple<>(String.valueOf(id), score));
                    }
                    if (!tuples.isEmpty()) {
                        redisService.setCacheZSet(hotRankKey, tuples);
                    }
                }
                redisService.deleteObject(tempKey);
            } catch (Exception e) {
                log.error("计算评价热度榜异常, type={}", type.getDesc(), e);
            }
        });
    }

    /**
     * 评价热度打分公式（参见《SmartLive 热度评分体系设计》第五章）
     *
     * hotScore = baseScore + liked × 0.4 + score × 0.3 + hasImage × 0.2 + timeDecay × 0.1
     * 初始分：无图 = 50, 有图 = 60
     * hasImage = 有图片 ? 10 : 0
     * timeDecay = max(0, 30 - 发布天数)
     *
     * @param type   评价类型
     * @param review 评价实体
     * @return 热度分（保留两位小数）
     */
    private double calcReviewHotScore(ReviewTypeEnum type, Review review) {
        // 初始分：有图评价 = 60分，无图评价 = 50分
        boolean hasImage = review.getImages() != null && !review.getImages().isEmpty();
        double baseScore = hasImage ? 60.0 : 50.0;

        // 时间衰减：30天内的新评价有额外加成
        double timeDecay = calcTimeDecay(review.getCreateTime());

        return roundScore(baseScore
                + safeInt(review.getLiked()) * 0.4          // 点赞数，用户认可度
                + safeInt(review.getScore()) * 0.3          // 评分高低
                + (hasImage ? 10 : 0) * 0.2                // 有图片 = 10分，无图 = 0分
                + timeDecay * 0.1                           // 新评价优先展示
        );
    }

    @Override
    public void fullRebuildRank() {
        log.info("开始全量重建评价热榜...");
        try {
            // 1. 查询所有正常状态的评价
            List<Review> allReviews = reviewMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                            .ne(Review::getAuditStatus, 2)
            );
            if (CollUtil.isEmpty(allReviews)) {
                log.warn("全量重建评价热榜：未获取到任何评价数据");
                return;
            }
            log.info("全量重建评价热榜：共获取到 {} 条评价", allReviews.size());

            // 2. 按 sourceType 和 sourceId 分组
            for (ReviewTypeEnum type : ReviewTypeEnum.values()) {
                RankRedisEnum redisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", type.getCode());
                if (redisEnum == null || redisEnum.getHotRankKeyPrefix() == null) continue;

                // 筛选当前类型的评价，按 sourceId 分组
                Map<Long, List<Review>> sourceGrouped = allReviews.stream()
                        .filter(r -> r.getSourceType() != null && r.getSourceType().equals(type.getCode()))
                        .filter(r -> r.getSourceId() != null)
                        .collect(Collectors.groupingBy(Review::getSourceId));

                for (Map.Entry<Long, List<Review>> entry : sourceGrouped.entrySet()) {
                    Long sourceId = entry.getKey();
                    List<Review> reviews = entry.getValue();
                    String hotRankKey = redisEnum.getHotRankKeyPrefix() + sourceId;

                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                    for (Review review : reviews) {
                        double score = calcReviewHotScore(type, review);
                        tuples.add(new DefaultTypedTuple<>(String.valueOf(review.getId()), score));
                    }

                    if (!tuples.isEmpty()) {
                        redisService.deleteObject(hotRankKey);
                        redisService.setCacheZSet(hotRankKey, tuples);
                    }
                }
                log.info("全量重建[{}]评价热榜完成，共处理 {} 个来源", type.getDesc(), sourceGrouped.size());
            }
        } catch (Exception e) {
            log.error("全量重建评价热榜异常", e);
        }
    }

    private Map<Long, Review> toReviewMap(List<Review> reviews) {
        if (CollUtil.isEmpty(reviews)) return Collections.emptyMap();
        return reviews.stream().filter(Objects::nonNull).filter(i -> i.getId() != null).collect(Collectors.toMap(Review::getId, i -> i, (a, b) -> a));
    }
}
