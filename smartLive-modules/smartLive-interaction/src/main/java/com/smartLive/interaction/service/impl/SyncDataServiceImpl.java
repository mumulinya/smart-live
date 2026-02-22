package com.smartLive.interaction.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.LikeTypeEnum;
import com.smartLive.common.core.enums.ReviewTypeEnum;
import com.smartLive.common.core.enums.StarTypeEnum;
import com.smartLive.common.redis.service.RedisService;
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
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * 互动数据同步实现
 */
@Service
@Slf4j
public class SyncDataServiceImpl implements ISyncDataService
{
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
    private ExecutorService executorService;

    @Override
    public void syncAllData()
    {
        log.info("开始执行互动数据同步主任务");
        long start = System.currentTimeMillis();

        CompletableFuture<Void> likeFuture = CompletableFuture.runAsync(this::syncLikeData, executorService);
        CompletableFuture<Void> commentFuture = CompletableFuture.runAsync(this::syncCommentData, executorService);
        CompletableFuture<Void> starFuture = CompletableFuture.runAsync(this::syncStarData, executorService);
        CompletableFuture<Void> reviewFuture = CompletableFuture.runAsync(this::syncReviewData, executorService);

        CompletableFuture.allOf(likeFuture, commentFuture, starFuture, reviewFuture).join();
        log.info("互动数据同步主任务结束，耗时:{}ms", System.currentTimeMillis() - start);
    }

    @Override
    public void syncLikeData()
    {
        log.info("开始同步点赞数");
        Arrays.stream(LikeTypeEnum.values()).forEach(type -> {
            if (type.getLikedCountKeyPrefix() == null || type.getLikeDirtyKeyPrefix() == null)
            {
                return;
            }
            LikeStrategy strategy = likeStrategyFactory.getStrategy(type.getCode());
            if (strategy == null)
            {
                log.warn("点赞类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String dirtyKey = type.getLikeDirtyKeyPrefix();
            sync(type.getDesc(), type.getLikedCountKeyPrefix(), dirtyKey, dirtyKey + ":TEMP",
                    strategy::transLikeCountFromRedis2DB);
        });
        log.info("同步点赞数完成");
    }

    @Override
    public void syncCommentData()
    {
        log.info("开始同步评论数");
        Arrays.stream(CommentTypeEnum.values()).forEach(type -> {
            if (type.getCommentCountKeyPrefix() == null || type.getCommentDirtyKeyPrefix() == null)
            {
                return;
            }
            CommentStrategy strategy = commentStrategyFactory.getStrategy(type.getCode());
            if (strategy == null)
            {
                log.warn("评论类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String dirtyKey = type.getCommentDirtyKeyPrefix();
            sync(type.getDesc(), type.getCommentCountKeyPrefix(), dirtyKey, dirtyKey + ":TEMP",
                    strategy::transCommentCountFromRedis2DB);
        });
        log.info("同步评论数完成");
    }

    @Override
    public void syncStarData()
    {
        log.info("开始同步收藏数");
        Arrays.stream(StarTypeEnum.values()).forEach(type -> {
            if (type.getStarCountKeyPrefix() == null || type.getStarDirtyKeyPrefix() == null)
            {
                return;
            }
            StarStrategy strategy = starStrategyFactory.getStrategy(type.getCode());
            if (strategy == null)
            {
                log.warn("收藏类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String dirtyKey = type.getStarDirtyKeyPrefix();
            sync(type.getDesc(), type.getStarCountKeyPrefix(), dirtyKey, dirtyKey + ":TEMP",
                    strategy::transStarCountFromRedis2DB);
        });
        log.info("同步收藏数完成");
    }

    @Override
    public void syncReviewData()
    {
        log.info("开始同步评价数");
        Arrays.stream(ReviewTypeEnum.values()).forEach(type -> {
            if (type.getReviewCountKeyPrefix() == null || type.getReviewDirtyKeyPrefix() == null)
            {
                return;
            }
            ReviewStrategy strategy = reviewStrategyFactory.getStrategy(type.getCode());
            if (strategy == null)
            {
                log.warn("评价类型[{}]没有对应策略，跳过", type.getDesc());
                return;
            }
            String dirtyKey = type.getReviewDirtyKeyPrefix();
            sync(type.getDesc(), type.getReviewCountKeyPrefix(), dirtyKey, dirtyKey + ":TEMP",
                    strategy::transReviewCountFromRedis2DB);
        });
        log.info("同步评价数完成");
    }

    private void sync(String desc, String countKeyPrefix, String dirtyKey, String tempKey,
            Consumer<Map<Long, Integer>> dbAction)
    {
        try
        {
            if (Boolean.FALSE.equals(redisService.hasKey(dirtyKey)))
            {
                log.debug("[{}]没有脏数据，跳过", desc);
                return;
            }

            redisService.rename(dirtyKey, tempKey);
            Set<Object> dirtyIds = redisService.getCacheSet(tempKey);
            if (CollUtil.isEmpty(dirtyIds))
            {
                redisService.deleteObject(tempKey);
                return;
            }

            Map<Long, Integer> updateMap = new HashMap<>(dirtyIds.size());
            for (Object idObj : dirtyIds)
            {
                Long id = Long.valueOf(idObj.toString());
                Object countObj = redisService.getCacheObject(countKeyPrefix + id);
                updateMap.put(id, countObj == null ? 0 : Integer.parseInt(countObj.toString()));
            }

            if (CollUtil.isNotEmpty(updateMap))
            {
                dbAction.accept(updateMap);
            }

            redisService.deleteObject(tempKey);
        }
        catch (Exception e)
        {
            // 保留 TEMP key 供下次排查或补偿
            log.error("同步[{}]数据失败", desc, e);
        }
    }
}
