package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.RankRedisEnum;
import com.smartLive.common.core.enums.ReviewTypeEnum;
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
 * 评价实体的全局统一热度计算策略
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
                        if (review.getStatus() != null && review.getStatus() == 2) {
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

    private double calcReviewHotScore(ReviewTypeEnum type, Review review) {
        double baseScoreWeight = 1.5D;
        double likeWeight = 2.0D;
        double replyWeight = 2.5D;
        if (type == ReviewTypeEnum.PRODUCT_REVIEW) {
            likeWeight = 1.8D;
            replyWeight = 3.0D;
        }
        int stars = safeInt(review.getScore());
        double interaction = stars * baseScoreWeight + safeInt(review.getLiked()) * likeWeight + safeInt(review.getReplyCount()) * replyWeight + 1.0D;
        return applyTimeDecay(interaction, review.getCreateTime() == null ? null : review.getCreateTime().getTime());
    }

    private Map<Long, Review> toReviewMap(List<Review> reviews) {
        if (CollUtil.isEmpty(reviews)) return Collections.emptyMap();
        return reviews.stream().filter(Objects::nonNull).filter(i -> i.getId() != null).collect(Collectors.toMap(Review::getId, i -> i, (a, b) -> a));
    }
}
