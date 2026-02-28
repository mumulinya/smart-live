package com.smartLive.interaction.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.FollowTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.RankRedisEnum;
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
import com.smartLive.interaction.strategy.factory.FollowStrategyFactory;
import com.smartLive.interaction.strategy.factory.LikeStrategyFactory;
import com.smartLive.interaction.strategy.factory.ReviewStrategyFactory;
import com.smartLive.interaction.strategy.factory.StarStrategyFactory;
import com.smartLive.interaction.strategy.follow.FollowStrategy;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import com.smartLive.interaction.strategy.review.ReviewStrategy;
import com.smartLive.interaction.strategy.star.StarStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
    private FollowStrategyFactory followStrategyFactory;
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
        CompletableFuture<Void> followFuture = CompletableFuture.runAsync(this::syncFollowData, executorService);
        CompletableFuture<Void> fansFuture = CompletableFuture.runAsync(this::syncFansData, executorService);
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
     * 从 Redis 独立计数器同步关注数到 MySQL。
     */
    @Override
    public void syncFollowData() {
        log.info("开始同步关注计数");
        Arrays.stream(FollowTypeEnum.values()).forEach(type -> {
            FollowStrategy strategy = followStrategyFactory.getStrategy(type.getCode());
            if (strategy == null) {
                log.warn("关注类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            // 同步关注计数（使用独立计数器 key）
            String followDirtyKey = type.getFollowDirtyKeyPrefix();
            if (type.getFollowCountKeyPrefix() != null && followDirtyKey != null) {
                sync(type.getDesc() + "-关注数",
                        type.getFollowCountKeyPrefix(),
                        followDirtyKey,
                        followDirtyKey + ":TEMP",
                        strategy::transFollowCountFromRedis2DB,
                        null);
            }
        });
        log.info("同步关注计数完成");
    }

    /**
     * 从 Redis 独立计数器同步粉丝数到 MySQL。
     */
    @Override
    public void syncFansData() {
        log.info("开始同步粉丝计数");
        Arrays.stream(FollowTypeEnum.values()).forEach(type -> {
            FollowStrategy strategy = followStrategyFactory.getStrategy(type.getCode());
            if (strategy == null) {
                log.warn("粉丝类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            // 同步粉丝计数（使用独立计数器 key）
            String fansDirtyKey = type.getFansDirtyKeyPrefix();
            if (type.getFansCountKeyPrefix() != null && fansDirtyKey != null) {
                sync(type.getDesc() + "-粉丝数",
                        type.getFansCountKeyPrefix(),
                        fansDirtyKey,
                        fansDirtyKey + ":TEMP",
                        strategy::transFansCountFromRedis2DB,
                        null);
            }
        });
        log.info("同步粉丝计数完成");
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
     * 点赞数据同步后，将受影响的目标源推入算分队列。
     * 逻辑：“父凭子贵”，评论/评价被点赞，其自身需要重新计算热度。
     *
     * @param likeType  点赞枚举类型
     * @param sourceIds 发生点赞变化的目标 ID 集合
     */
    private void enqueueCalcAfterLikeSync(LikeTypeEnum likeType, Collection<Long> sourceIds) {
        if (CollUtil.isEmpty(sourceIds) || likeType == null) {
            return;
        }

        if (Objects.equals(likeType.getCode(),GlobalBizTypeEnum.COMMENT.getCode())) {
            List<Comment> comments = commentMapper.selectBatchIds(sourceIds);
            if (CollUtil.isEmpty(comments)) return;
            
            for (Comment comment : comments) {
                if (comment == null || comment.getId() == null) continue;
                RankRedisEnum rankRedisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", comment.getSourceType());
                if (rankRedisEnum != null && rankRedisEnum.getCalcQueueKey() != null) {
                    redisService.setCacheSet(rankRedisEnum.getCalcQueueKey(), comment.getId().toString());
                }
            }
            return;
        }

        if (Objects.equals(likeType.getCode(), GlobalBizTypeEnum.REVIEW.getCode())) {
            enqueueReviewIdsToCalcBySourceType(sourceIds);
            return;
        }
        
        // 实体本体被点赞（如博客），通过 "ENTITY" 寻址本体专属的 RankRedisEnum
        RankRedisEnum entityRankEnum = RankRedisEnum.getByCategoryAndCode("ENTITY", likeType.getCode());
        if (entityRankEnum != null && entityRankEnum.getCalcQueueKey() != null) {
            enqueueIds(entityRankEnum.getCalcQueueKey(), sourceIds);
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
            RankRedisEnum rankRedisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", commentType.getCode());
            if (rankRedisEnum != null && rankRedisEnum.getCalcQueueKey() != null) {
                enqueueIds(rankRedisEnum.getCalcQueueKey(), sourceIds);
            }
            return;
        }
        if (commentType == CommentTypeEnum.REVIEW_COMMENT) {
            enqueueReviewIdsToCalcBySourceType(sourceIds);
            return;
        }
        
        // 实体本体被评论（如博客底下的直接评论），借由 "ENTITY" 和 code 获取本体自身热度队列
        RankRedisEnum entityRankEnum = RankRedisEnum.getByCategoryAndCode("ENTITY", commentType.getCode());
        if (entityRankEnum != null && entityRankEnum.getCalcQueueKey() != null) {
            enqueueIds(entityRankEnum.getCalcQueueKey(), sourceIds);
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
        if (Objects.equals(starType.getCode(), GlobalBizTypeEnum.REVIEW.getCode())) {
            enqueueReviewIdsToCalcBySourceType(sourceIds);
            return;
        }
        
        RankRedisEnum entityRankEnum = RankRedisEnum.getByCategoryAndCode("ENTITY", starType.getCode());
        if (entityRankEnum != null && entityRankEnum.getCalcQueueKey() != null) {
            enqueueIds(entityRankEnum.getCalcQueueKey(), sourceIds);
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
            RankRedisEnum rankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", review.getSourceType());
            if (rankRedisEnum != null && rankRedisEnum.getCalcQueueKey() != null) {
                redisService.setCacheSet(rankRedisEnum.getCalcQueueKey(), review.getId().toString());
            }
        }
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
