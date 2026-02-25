package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.domain.VO.BlogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 博客本身的人气榜，按收藏/点赞独立计算 
 */
@Slf4j
@Component
public class BlogHotRankStrategyImpl extends AbstractHotRankStrategy {

    @Autowired
    private RemoteBlogService remoteBlogService;
    
    @Override
    public Integer getType() { 
        return GlobalBizTypeEnum.BLOG.getCode(); 
    }
    
    @Override
    public void calculateAndRefreshRank() {
        String calcKey = RedisConstants.BLOG_CALC_QUEUE_KEY;
        String tempKey = calcKey + ":TEMP";
        try {
            if (Boolean.FALSE.equals(redisService.hasKey(calcKey))) return;
            
            redisService.rename(calcKey, tempKey);
            Set<Object> activeSet = redisService.getCacheSet(tempKey);
            if (CollUtil.isEmpty(activeSet)) {
                redisService.deleteObject(tempKey);
                return;
            }

            Set<Long> candidateIds = toLongSet(activeSet);
            String hotRankKey = RedisConstants.BLOG_HOT_RANK_KEY;
            candidateIds.addAll(getTopIds(hotRankKey, HOT_RANK_MERGE_TOP_N));
            
            List<BlogVO> blogList = getResourceList(getType(), new ArrayList<>(candidateIds));
            if (CollUtil.isEmpty(blogList)) {
                redisService.deleteObject(tempKey);
                return;
            }

            Map<Long, BlogVO> blogMap = blogList.stream().collect(Collectors.toMap(BlogVO::getId, b -> b, (k1, k2) -> k1));
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
            
            for (Long id : candidateIds) {
                BlogVO blog = blogMap.get(id);
                if (blog == null) {
                    redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                    continue;
                }
                
                double score = calcBlogScore(blog);
                tuples.add(new DefaultTypedTuple<>(String.valueOf(id), score));
            }
            
            if (!tuples.isEmpty()) {
                redisService.setCacheZSet(hotRankKey, tuples);
            }
            redisService.deleteObject(tempKey);
        } catch (Exception e) {
            log.error("计算博客热度榜异常", e);
        }
    }

    @Override
    public void fullRebuildRank() {
        log.info("开始全量重建博客热榜...");
        String hotRankKey = RedisConstants.BLOG_HOT_RANK_KEY;
        try {
            // 1. 获取全部博客 ID
            List<Long> allIds = remoteBlogService.getAllBlogIds();
            if (CollUtil.isEmpty(allIds)) {
                log.warn("全量重建博客热榜：未获取到任何博客ID");
                return;
            }
            log.info("全量重建博客热榜：共获取到 {} 个博客ID", allIds.size());

            // 2. 分批获取博客详情并计算分数
            Set<ZSetOperations.TypedTuple<String>> allTuples = new HashSet<>();
            List<List<Long>> partitions = ListUtil.partition(allIds, 200);
            for (List<Long> batch : partitions) {
                List<BlogVO> blogList = getResourceList(getType(), batch);
                if (CollUtil.isEmpty(blogList)) continue;

                for (BlogVO blog : blogList) {
                    double score = calcBlogScore(blog);
                    allTuples.add(new DefaultTypedTuple<>(String.valueOf(blog.getId()), score));
                }
            }

            // 3. 原子性替换：先删旧 ZSet，再写入新数据
            if (!allTuples.isEmpty()) {
                redisService.deleteObject(hotRankKey);
                redisService.setCacheZSet(hotRankKey, allTuples);
                log.info("全量重建博客热榜完成：共写入 {} 条数据", allTuples.size());
            }
        } catch (Exception e) {
            log.error("全量重建博客热榜异常", e);
        }
    }

    /**
     * 博客热度打分公式（复用于增量和全量）
     */
    private double calcBlogScore(BlogVO blog) {
        double likeWeight = 1.5D;
        double commentWeight = 2.0D;
        double interaction = safeInt(blog.getLiked()) * likeWeight + safeInt(blog.getComments()) * commentWeight + 1.0D;
        return applyTimeDecay(interaction, blog.getPublishTime() == null ? null : blog.getPublishTime().getTime());
    }
}
