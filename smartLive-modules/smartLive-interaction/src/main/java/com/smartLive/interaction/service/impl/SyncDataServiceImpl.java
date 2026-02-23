package com.smartLive.interaction.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.LikeTypeEnum;
import com.smartLive.common.core.enums.ReviewTypeEnum;
import com.smartLive.common.core.enums.StarTypeEnum;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.mapper.CommentMapper;
import com.smartLive.interaction.mapper.ReviewMapper;
import com.smartLive.interaction.service.ISyncDataService;
import com.smartLive.interaction.strategy.comment.CommentStrategy;
import com.smartLive.interaction.strategy.factory.CommentStrategyFactory;
import com.smartLive.interaction.strategy.factory.LikeStrategyFactory;
import com.smartLive.interaction.strategy.factory.ReviewStrategyFactory;
import com.smartLive.interaction.strategy.factory.StarStrategyFactory;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import com.smartLive.interaction.strategy.review.ReviewStrategy;
import com.smartLive.interaction.strategy.star.StarStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 互动数据同步与热度计算核心实现类
 * 采用“双轨制异步架构”：
 * 1. 同步轨 (Sync): 将高频产生的互动数据（点赞、收藏、评论数）批量刷入 MySQL。
 * 2. 算分轨 (Calc): 基于时间衰减算法，动态重算业务主体的热度分，并洗牌 Redis ZSet 排行榜。
 */
@Service
@Slf4j
public class SyncDataServiceImpl implements ISyncDataService {

    /** 热度榜计算时，需要拉取参与重算的老数据前 N 名（防霸榜机制） */
    private static final int HOT_RANK_MERGE_TOP_N = 50;
    /** 热度衰减算法参数：基础缓冲时间（小时） */
    private static final double HOT_SCORE_BASE_HOURS = 2.0D;
    /** 热度衰减算法参数：重力衰减因子（值越大，老数据降分越快） */
    private static final double HOT_SCORE_GRAVITY = 1.2D;

    @Autowired
    private RedisService redisService;
    @Autowired
    private LikeStrategyFactory likeStrategyFactory;
    @Autowired
    private CommentStrategyFactory commentStrategyFactory;
    @Autowired
    private StarStrategyFactory starStrategyFactory;
    @Autowired
    private ReviewStrategyFactory reviewStrategyFactory;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ExecutorService executorService;

    /**
     * 定时任务主入口：并发执行全量数据的落库同步与热度重算。
     */
    @Override
    public void syncAllData() {
        log.info("开始执行互动数据同步主任务");
        long start = System.currentTimeMillis();

        CompletableFuture<Void> likeFuture = CompletableFuture.runAsync(this::syncLikeData, executorService);
        CompletableFuture<Void> commentFuture = CompletableFuture.runAsync(this::syncCommentData, executorService);
        CompletableFuture<Void> starFuture = CompletableFuture.runAsync(this::syncStarData, executorService);
        CompletableFuture<Void> reviewFuture = CompletableFuture.runAsync(this::syncReviewData, executorService);
        CompletableFuture<Void> hotRankFuture = CompletableFuture.runAsync(this::calcHotRankData, executorService);

        CompletableFuture.allOf(likeFuture, commentFuture, starFuture, reviewFuture, hotRankFuture).join();
        log.info("互动数据同步主任务结束，耗时:{}ms", System.currentTimeMillis() - start);
    }

    /**
     * 从 Redis 同步点赞数到 MySQL，完成后将受影响的主体推入算分队列。
     */
    @Override
    public void syncLikeData() {
        log.info("开始同步点赞数");
        Arrays.stream(LikeTypeEnum.values()).forEach(type -> {
            if (type.getLikedCountKeyPrefix() == null || type.getLikeDirtyKeyPrefix() == null) {
                return;
            }
            LikeStrategy strategy = likeStrategyFactory.getStrategy(type.getCode());
            if (strategy == null) {
                log.warn("点赞类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String dirtyKey = type.getLikeDirtyKeyPrefix();
            sync(type.getDesc(), type.getLikedCountKeyPrefix(), dirtyKey, dirtyKey + ":TEMP",
                    strategy::transLikeCountFromRedis2DB,
                    updateMap -> enqueueCalcAfterLikeSync(type, updateMap.keySet()));
        });
        log.info("同步点赞数完成");
    }

    /**
     * 从 Redis 同步评论数到 MySQL，完成后将受影响的主体推入算分队列。
     */
    @Override
    public void syncCommentData() {
        log.info("开始同步评论数");
        Arrays.stream(CommentTypeEnum.values()).forEach(type -> {
            if (type.getCommentCountKeyPrefix() == null || type.getCommentSyncKey() == null) {
                return;
            }
            CommentStrategy strategy = commentStrategyFactory.getStrategy(type.getCode());
            if (strategy == null) {
                log.warn("评论类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String syncKey = type.getCommentSyncKey();
            sync(type.getDesc(), type.getCommentCountKeyPrefix(), syncKey, syncKey + ":TEMP",
                    strategy::transCommentCountFromRedis2DB,
                    updateMap -> enqueueCalcAfterCommentSync(type, updateMap.keySet()));
        });
        log.info("同步评论数完成");
    }

    /**
     * 从 Redis 同步收藏数到 MySQL，完成后将受影响的主体推入算分队列。
     */
    @Override
    public void syncStarData() {
        log.info("开始同步收藏数");
        Arrays.stream(StarTypeEnum.values()).forEach(type -> {
            if (type.getStarCountKeyPrefix() == null || type.getStarDirtyKeyPrefix() == null) {
                return;
            }
            StarStrategy strategy = starStrategyFactory.getStrategy(type.getCode());
            if (strategy == null) {
                log.warn("收藏类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String dirtyKey = type.getStarDirtyKeyPrefix();
            sync(type.getDesc(), type.getStarCountKeyPrefix(), dirtyKey, dirtyKey + ":TEMP",
                    strategy::transStarCountFromRedis2DB,
                    updateMap -> enqueueCalcAfterStarSync(type, updateMap.keySet()));
        });
        log.info("同步收藏数完成");
    }

    /**
     * 从 Redis 同步评价数到 MySQL。
     */
    @Override
    public void syncReviewData() {
        log.info("开始同步评价数");
        Arrays.stream(ReviewTypeEnum.values()).forEach(type -> {
            if (type.getReviewCountKeyPrefix() == null || type.getReviewSyncKey() == null) {
                return;
            }
            ReviewStrategy strategy = reviewStrategyFactory.getStrategy(type.getCode());
            if (strategy == null) {
                log.warn("评价类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String syncKey = type.getReviewSyncKey();
            sync(type.getDesc(), type.getReviewCountKeyPrefix(), syncKey, syncKey + ":TEMP",
                    strategy::transReviewCountFromRedis2DB,
                    null);
        });
        log.info("同步评价数完成");
    }

    /**
     * 通用的 Redis 快照轮转与同步落库模型。
     * 使用 RENAME 原子操作保证高并发下数据不丢失。
     *
     * @param desc              数据描述（用于日志）
     * @param countKeyPrefix    数值计数器的 Redis Key 前缀
     * @param dirtyKey          待同步的脏数据集合 Key (Sync Queue)
     * @param tempKey           处理时的临时快照 Key
     * @param dbAction          执行落库操作的函数
     * @param afterSyncAction   落库成功后的回调函数（通常用于推入算分队列）
     */
    private void sync(String desc,
                      String countKeyPrefix,
                      String dirtyKey,
                      String tempKey,
                      Consumer<Map<Long, Integer>> dbAction,
                      Consumer<Map<Long, Integer>> afterSyncAction) {
        try {
            if (Boolean.FALSE.equals(redisService.hasKey(dirtyKey))) {
                log.debug("[{}]没有待同步数据，跳过", desc);
                return;
            }

            if (Boolean.TRUE.equals(redisService.hasKey(tempKey))) {
                redisService.deleteObject(tempKey);
            }
            // 核心：原子重命名，截取当前快照，新请求将自动生成新的 dirtyKey
            redisService.rename(dirtyKey, tempKey);

            Set<Object> dirtyIds = redisService.getCacheSet(tempKey);
            if (CollUtil.isEmpty(dirtyIds)) {
                redisService.deleteObject(tempKey);
                return;
            }

            Map<Long, Integer> updateMap = new HashMap<>(dirtyIds.size());
            for (Object idObj : dirtyIds) {
                if (idObj == null) {
                    continue;
                }
                Long id = Long.valueOf(idObj.toString());
                Object countObj = redisService.getCacheObject(countKeyPrefix + id);
                updateMap.put(id, countObj == null ? 0 : Integer.parseInt(countObj.toString()));
            }

            if (CollUtil.isNotEmpty(updateMap)) {
                dbAction.accept(updateMap);
                if (afterSyncAction != null) {
                    afterSyncAction.accept(updateMap);
                }
            }

            redisService.deleteObject(tempKey);
        } catch (Exception e) {
            log.error("[{}] 数据同步异常，保留 TEMP key 以备排查", desc, e);
        }
    }

    /**
     * 重算各业务线的排行榜热度分。
     */
    @Override
    public void calcHotRankData() {
        log.info("开始重算评论/评价热度榜");
        Arrays.stream(CommentTypeEnum.values()).forEach(this::calcCommentHotRank);
        Arrays.stream(ReviewTypeEnum.values()).forEach(this::calcReviewHotRank);
        log.info("重算评论/评价热度榜完成");
    }

    /**
     * 点赞数据同步后，将受影响的目标源推入算分队列。
     * 逻辑：“父凭子贵”，评论/评价被点赞，其自身需要重新计算热度。
     *
     * @param likeType  点赞枚举类型
     * @param sourceIds 发生点赞变化的目标 ID 集合
     */
    private void enqueueCalcAfterLikeSync(LikeTypeEnum likeType, Collection<Long> sourceIds) {
        if (CollUtil.isEmpty(sourceIds)) {
            return;
        }
        if (Objects.equals(likeType.getCode(), com.smartLive.common.core.enums.GlobalBizTypeEnum.COMMENT.getCode())) {
            List<Comment> comments = commentMapper.selectBatchIds(sourceIds);
            if (CollUtil.isEmpty(comments)) {
                return;
            }
            for (Comment comment : comments) {
                if (comment == null || comment.getId() == null) {
                    continue;
                }
                CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
                if (commentType != null) {
                    redisService.setCacheSet(commentType.getCommentCalcKey(), comment.getId().toString());
                }
            }
            return;
        }

        if (Objects.equals(likeType.getCode(), com.smartLive.common.core.enums.GlobalBizTypeEnum.REVIEW.getCode())) {
            enqueueReviewIdsToCalcBySourceType(sourceIds);
        }
    }

    /**
     * 评论数据同步后，将受影响的目标源推入算分队列。
     * * @param commentType 评论枚举类型
     * @param sourceIds   被评论的目标 ID 集合
     */
    private void enqueueCalcAfterCommentSync(CommentTypeEnum commentType, Collection<Long> sourceIds) {
        if (commentType == null || CollUtil.isEmpty(sourceIds)) {
            return;
        }
        if (commentType == CommentTypeEnum.COMMENT_COMMENT) {
            enqueueIds(commentType.getCommentCalcKey(), sourceIds);
            return;
        }
        if (commentType == CommentTypeEnum.REVIEW_COMMENT) {
            enqueueReviewIdsToCalcBySourceType(sourceIds);
        }
    }

    /**
     * 收藏数据同步后，将受影响的目标源推入算分队列。
     *
     * @param starType  收藏枚举类型
     * @param sourceIds 被收藏的目标 ID 集合
     */
    private void enqueueCalcAfterStarSync(StarTypeEnum starType, Collection<Long> sourceIds) {
        if (starType == null || CollUtil.isEmpty(sourceIds)) {
            return;
        }
        if (Objects.equals(starType.getCode(), com.smartLive.common.core.enums.GlobalBizTypeEnum.REVIEW.getCode())) {
            enqueueReviewIdsToCalcBySourceType(sourceIds);
        }
    }

    /**
     * 将评价 ID 按其来源类型推入对应的算分队列中。
     *
     * @param reviewIds 评价 ID 集合
     */
    private void enqueueReviewIdsToCalcBySourceType(Collection<Long> reviewIds) {
        if (CollUtil.isEmpty(reviewIds)) {
            return;
        }
        List<Review> reviewList = reviewMapper.selectBatchIds(reviewIds);
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        for (Review review : reviewList) {
            if (review == null || review.getId() == null) {
                continue;
            }
            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
            if (reviewType == null) {
                continue;
            }
            redisService.setCacheSet(reviewType.getReviewCalcKey(), review.getId().toString());
        }
    }

    /**
     * 执行评论体系的热度计算与排行榜洗牌。
     * 核心逻辑：获取活跃评论 + 拉取原有霸榜前N名老评论 -> 统一应用衰减算法 -> 更新 ZSet。
     *
     * @param type 评论枚举类型
     */
    private void calcCommentHotRank(CommentTypeEnum type) {
        if (type == null || type.getCommentCalcKey() == null) {
            return;
        }
        String calcKey = type.getCommentCalcKey();
        String tempKey = calcKey + ":TEMP";

        try {
            if (Boolean.FALSE.equals(redisService.hasKey(calcKey))) {
                return;
            }
            if (Boolean.TRUE.equals(redisService.hasKey(tempKey))) {
                redisService.deleteObject(tempKey);
            }
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

            // 按 sourceId（如对应的博客ID）进行分组
            Map<Long, Set<Long>> sourceIdToIds = new LinkedHashMap<>();
            for (Comment comment : activeCommentMap.values()) {
                if (comment == null || comment.getId() == null || comment.getSourceId() == null) {
                    continue;
                }
                if (!Objects.equals(comment.getSourceType(), type.getCode())) {
                    continue;
                }
                sourceIdToIds.computeIfAbsent(comment.getSourceId(), k -> new LinkedHashSet<>()).add(comment.getId());
            }

            // 遍历每个被评论的主体，更新其名下的评论排行榜
            for (Map.Entry<Long, Set<Long>> entry : sourceIdToIds.entrySet()) {
                Long sourceId = entry.getKey();
                Set<Long> candidateIds = new LinkedHashSet<>(entry.getValue());
                String hotRankKey = type.getCommentHotRankKeyPrefix() + sourceId;

                // 【防霸榜机制】强制拉取原有榜单前 N 名，使其被动接受时间衰减的制裁
                candidateIds.addAll(getTopIds(hotRankKey, HOT_RANK_MERGE_TOP_N));

                Map<Long, Comment> candidateMap = toCommentMap(commentMapper.selectBatchIds(candidateIds));
                Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                for (Long id : candidateIds) {
                    Comment comment = candidateMap.get(id);
                    // 脏数据或所属源不匹配，移出排行榜
                    if (comment == null || comment.getSourceId() == null || !Objects.equals(comment.getSourceId(), sourceId)) {
                        redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                        continue;
                    }
                    // 状态为 2 (例如已删除或封禁)，移出排行榜和最新榜
                    if ("2".equals(comment.getStatus())) {
                        redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                        redisService.removeCacheZSetObject(type.getCommentNewRankKeyPrefix() + sourceId, String.valueOf(id));
                        continue;
                    }
                    // 应用算法计算最新分数
                    double score = calcCommentHotScore(type, comment);
                    tuples.add(new DefaultTypedTuple<>(String.valueOf(id), score));
                }
                // 批量洗牌写入 ZSet
                if (!tuples.isEmpty()) {
                    redisService.setCacheZSet(hotRankKey, tuples);
                }
            }

            redisService.deleteObject(tempKey);
        } catch (Exception e) {
            log.error("计算评论热度榜异常, type={}", type.name(), e);
        }
    }

    /**
     * 执行评价体系（如商铺评价）的热度计算与排行榜洗牌。
     * 逻辑同 {@link #calcCommentHotRank(CommentTypeEnum)}。
     *
     * @param type 评价枚举类型
     */
    private void calcReviewHotRank(ReviewTypeEnum type) {
        if (type == null || type.getReviewCalcKey() == null) {
            return;
        }
        String calcKey = type.getReviewCalcKey();
        String tempKey = calcKey + ":TEMP";

        try {
            if (Boolean.FALSE.equals(redisService.hasKey(calcKey))) {
                return;
            }
            if (Boolean.TRUE.equals(redisService.hasKey(tempKey))) {
                redisService.deleteObject(tempKey);
            }
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
                if (review == null || review.getId() == null || review.getSourceId() == null) {
                    continue;
                }
                if (!Objects.equals(review.getSourceType(), type.getCode())) {
                    continue;
                }
                sourceIdToIds.computeIfAbsent(review.getSourceId(), k -> new LinkedHashSet<>()).add(review.getId());
            }

            for (Map.Entry<Long, Set<Long>> entry : sourceIdToIds.entrySet()) {
                Long sourceId = entry.getKey();
                Set<Long> candidateIds = new LinkedHashSet<>(entry.getValue());
                String hotRankKey = type.getReviewHotRankKeyPrefix() + sourceId;

                candidateIds.addAll(getTopIds(hotRankKey, HOT_RANK_MERGE_TOP_N));

                Map<Long, Review> candidateMap = toReviewMap(reviewMapper.selectBatchIds(candidateIds));
                Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                for (Long id : candidateIds) {
                    Review review = candidateMap.get(id);
                    if (review == null || review.getSourceId() == null || !Objects.equals(review.getSourceId(), sourceId)) {
                        redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                        continue;
                    }
                    // 状态为 2 则清理该数据
                    if (review.getStatus() != null && review.getStatus() == 2) {
                        redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                        redisService.removeCacheZSetObject(type.getReviewNewRankKeyPrefix() + sourceId, String.valueOf(id));
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
            log.error("计算评价热度榜异常, type={}", type.name(), e);
        }
    }

    /**
     * 计算单条评论的综合质量分（分子部分）。
     * 根据业务类型分配不同的点赞和回复权重。
     *
     * @param type    评论枚举类型
     * @param comment 评论实体
     * @return 计算后的热度基础分
     */
    private double calcCommentHotScore(CommentTypeEnum type, Comment comment) {
        double likeWeight = 1.0D;
        double replyWeight = 2.0D;

        if (type == CommentTypeEnum.REVIEW_COMMENT) {
            likeWeight = 1.2D;
            replyWeight = 1.5D;
        } else if (type == CommentTypeEnum.COMMENT_COMMENT) {
            likeWeight = 1.0D;
            replyWeight = 1.2D;
        }

        double interaction =
                safeInt(comment.getLiked()) * likeWeight +
                        safeInt(comment.getReplyCount()) * replyWeight +
                        1.0D;

        return applyTimeDecay(interaction, comment.getCreateTime() == null ? null : comment.getCreateTime().getTime());
    }

    /**
     * 计算单条评价的综合质量分（分子部分）。
     * 评价算法更复杂，融入了星级打分、收藏量等维度。
     *
     * @param type   评价枚举类型
     * @param review 评价实体
     * @return 计算后的热度基础分
     */
    private double calcReviewHotScore(ReviewTypeEnum type, Review review) {
        double likeWeight = 1.1D;
        double replyWeight = 2.2D;
        double starWeight = 1.5D;
        double ratingWeight = type == ReviewTypeEnum.SHOP_REVIEW ? 1.3D : 0.8D;

        double rating = safeInt(review.getScore());
        // 商铺评价：提取环境、口味、服务综合评分作为权重维度
        if (type == ReviewTypeEnum.SHOP_REVIEW) {
            rating = (safeInt(review.getServiceScore()) + safeInt(review.getTasteScore()) + safeInt(review.getEnvScore())) / 3.0D;
            if (rating <= 0D) {
                rating = safeInt(review.getScore());
            }
        }

        double interaction =
                safeInt(review.getLiked()) * likeWeight +
                        safeInt(review.getReplyCount()) * replyWeight +
                        safeInt(review.getStared()) * starWeight +
                        rating * ratingWeight +
                        1.0D;

        return applyTimeDecay(interaction, review.getCreateTime() == null ? null : review.getCreateTime().getTime());
    }

    /**
     * 【核心算法】应用牛顿冷却/重力时间衰减公式。
     * 随着时间流逝，分母呈指数级增长，压低总分，从而给新数据出头之日。
     *
     * @param interactionScore 综合互动分（分子）
     * @param createTimeMillis 数据发布时间戳
     * @return 最终的热度排序分
     */
    private double applyTimeDecay(double interactionScore, Long createTimeMillis) {
        long now = System.currentTimeMillis();
        long createAt = createTimeMillis == null ? now : createTimeMillis;
        double hours = Math.max(0D, (now - createAt) / 3600000D);
        return interactionScore / Math.pow(hours + HOT_SCORE_BASE_HOURS, HOT_SCORE_GRAVITY);
    }

    /**
     * 安全的 Integer 转换，防止 NPE。
     *
     * @param value 数值对象
     * @return 转换后的 int 值，null 返回 0
     */
    private int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * 从 ZSet 排行榜中拉取当前排名前 N 的 ID 集合。
     *
     * @param hotRankKey 排行榜 Redis Key
     * @param topN       提取前 N 名
     * @return 排名靠前的 ID 集合
     */
    private Set<Long> getTopIds(String hotRankKey, int topN) {
        Set<Object> raw = redisService.getCacheZSetReverseRange(hotRankKey, 0, topN - 1L);
        return toLongSet(raw);
    }

    /**
     * 将评论列表转换为以 ID 为 Key 的 Map 结构，过滤空数据。
     *
     * @param comments 评论列表
     * @return 过滤后的 Map 结构
     */
    private Map<Long, Comment> toCommentMap(List<Comment> comments) {
        if (CollUtil.isEmpty(comments)) {
            return Collections.emptyMap();
        }
        return comments.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Comment::getId, item -> item, (a, b) -> a));
    }

    /**
     * 将评价列表转换为以 ID 为 Key 的 Map 结构，过滤空数据。
     *
     * @param reviews 评价列表
     * @return 过滤后的 Map 结构
     */
    private Map<Long, Review> toReviewMap(List<Review> reviews) {
        if (CollUtil.isEmpty(reviews)) {
            return Collections.emptyMap();
        }
        return reviews.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Review::getId, item -> item, (a, b) -> a));
    }

    /**
     * 将 Redis 取出的 Object 集合安全转换为 Long 类型的 Set。
     *
     * @param rawSet 原始对象集合
     * @return 转换后的 Long 集合
     */
    private Set<Long> toLongSet(Set<Object> rawSet) {
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

    /**
     * 批量将 ID 存入 Redis Set 集合中。
     *
     * @param key Redis Set 的 Key
     * @param ids ID 集合
     */
    private void enqueueIds(String key, Collection<Long> ids) {
        if (key == null || CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                redisService.setCacheSet(key, id.toString());
            }
        }
    }
}