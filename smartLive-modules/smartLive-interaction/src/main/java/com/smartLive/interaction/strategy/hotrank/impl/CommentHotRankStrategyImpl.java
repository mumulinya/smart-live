package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.RankRedisEnum;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.mapper.CommentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论热度计算策略实现
 * 
 * 核心逻辑：
 * 1. 自动对一级评论（parentId=0）进行热度排序。
 * 2. 评分维度：点赞数（用户共鸣度）、回复数（讨论激烈度）及发布时间。
 * 3. 旨在将高质量、有深度的见解推送到评论区顶部。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Component
public class CommentHotRankStrategyImpl extends AbstractHotRankStrategy {

    @Autowired
    protected RedisService redisService;
    @Autowired
    protected CommentMapper commentMapper;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.COMMENT.getCode();
    }

    /**
     * 增量刷新评论热度榜
     */
    @Override
    public void calculateAndRefreshRank() {
        Arrays.stream(CommentTypeEnum.values()).forEach(type -> {
            RankRedisEnum redisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", type.getCode());
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
                Map<Long, Comment> activeCommentMap = toCommentMap(commentMapper.selectBatchIds(activeIds));
                if (activeCommentMap.isEmpty()) {
                    redisService.deleteObject(tempKey);
                    return;
                }

                Map<Long, Set<Long>> sourceIdToIds = new LinkedHashMap<>();
                for (Comment comment : activeCommentMap.values()) {
                    if (comment == null || comment.getId() == null || comment.getSourceId() == null) continue;
                    if (!Objects.equals(comment.getSourceType(), type.getCode())) continue;
                    sourceIdToIds.computeIfAbsent(comment.getSourceId(), k -> new LinkedHashSet<>()).add(comment.getId());
                }

                for (Map.Entry<Long, Set<Long>> entry : sourceIdToIds.entrySet()) {
                    Long sourceId = entry.getKey();
                    Set<Long> candidateIds = new LinkedHashSet<>(entry.getValue());
                    String hotRankKey = redisEnum.getHotRankKeyPrefix() + sourceId;

                    candidateIds.addAll(getTopIds(hotRankKey, HOT_RANK_MERGE_TOP_N));
                    
                    Map<Long, Comment> candidateMap = toCommentMap(commentMapper.selectBatchIds(candidateIds));
                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();

                    for (Long id : candidateIds) {
                        Comment comment = candidateMap.get(id);
                        if (comment == null || !ObjectUtil.equals(comment.getSourceId(), sourceId)) {
                            redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                            continue;
                        }
                        if ("2".equals(comment.getStatus())) {
                            redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                            if (redisEnum.getNewRankKeyPrefix() != null) {
                                redisService.removeCacheZSetObject(redisEnum.getNewRankKeyPrefix() + sourceId, String.valueOf(id));
                            }
                            continue;
                        }
                        double score = calcCommentHotScore(type, comment);
                        tuples.add(new DefaultTypedTuple<>(String.valueOf(id), score));
                    }
                    if (!tuples.isEmpty()) {
                        redisService.setCacheZSet(hotRankKey, tuples);
                    }
                }
                redisService.deleteObject(tempKey);
            } catch (Exception e) {
                log.error("计算评论热度榜异常, type={}", type.getDesc(), e);
            }
        });
    }

    /**
     * 评论热度打分公式（参见《SmartLive 热度评分体系设计》第六章）
     *
     * hotScore = baseScore + liked × 0.4 + replyCount × 0.4 + timeDecay × 0.2
     * 初始分：普通评论 = 50
     * timeDecay = max(0, 30 - 发布天数)
     *
     * @param type    评论类型
     * @param comment 评论实体
     * @return 热度分（保留两位小数）
     */
    private double calcCommentHotScore(CommentTypeEnum type, Comment comment) {
        // 初始分：普通评论 = 50分
        double baseScore = 50.0;

        // 时间衰减：30天内的新评论有额外加成
        double timeDecay = calcTimeDecay(comment.getCreateTime());

        return roundScore(baseScore
                + safeInt(comment.getLiked()) * 0.4       // 点赞数
                + safeInt(comment.getReplyCount()) * 0.4  // 回复数，引发讨论的评论更有价值
                + timeDecay * 0.2                         // 新评论优先
        );
    }

    /**
     * 全量重建所有评论热榜
     */
    @Override
    public void fullRebuildRank() {
        log.info("开始全量重建评论热榜...");
        try {
            // 1. 查询所有正常状态的评论（排除 status=2 禁止查看的）
            List<Comment> allComments = commentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Comment>()
                            .ne(Comment::getStatus, 2)
                            .eq(Comment::getParentId, 0L)
            );
            if (CollUtil.isEmpty(allComments)) {
                log.warn("全量重建评论热榜：未获取到任何评论数据");
                return;
            }
            log.info("全量重建评论热榜：共获取到 {} 条一级评论", allComments.size());

            // 2. 按 sourceType 和 sourceId 分组
            for (CommentTypeEnum type : CommentTypeEnum.values()) {
                RankRedisEnum redisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", type.getCode());
                if (redisEnum == null || redisEnum.getHotRankKeyPrefix() == null) continue;

                // 筛选当前类型的评论，按 sourceId 分组
                Map<Long, List<Comment>> sourceGrouped = allComments.stream()
                        .filter(c -> c.getSourceType() != null && c.getSourceType().equals(type.getCode()))
                        .filter(c -> c.getSourceId() != null)
                        .collect(Collectors.groupingBy(Comment::getSourceId));

                for (Map.Entry<Long, List<Comment>> entry : sourceGrouped.entrySet()) {
                    Long sourceId = entry.getKey();
                    List<Comment> comments = entry.getValue();
                    String hotRankKey = redisEnum.getHotRankKeyPrefix() + sourceId;

                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                    for (Comment comment : comments) {
                        double score = calcCommentHotScore(type, comment);
                        tuples.add(new DefaultTypedTuple<>(String.valueOf(comment.getId()), score));
                    }

                    if (!tuples.isEmpty()) {
                        redisService.deleteObject(hotRankKey);
                        redisService.setCacheZSet(hotRankKey, tuples);
                    }
                }
                log.info("全量重建[{}]评论热榜完成，共处理 {} 个来源", type.getDesc(), sourceGrouped.size());
            }
        } catch (Exception e) {
            log.error("全量重建评论热榜异常", e);
        }
    }

    private Map<Long, Comment> toCommentMap(List<Comment> comments) {
        if (CollUtil.isEmpty(comments)) return Collections.emptyMap();
        return comments.stream().filter(Objects::nonNull).filter(i -> i.getId() != null).collect(Collectors.toMap(Comment::getId, i -> i, (a, b) -> a));
    }
}
