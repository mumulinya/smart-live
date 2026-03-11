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
 * 博客热度计算策略实现
 * 
 * 核心逻辑：
 * 1. 监听 Redis 计算队列中的活跃博客 ID。
 * 2. 结合点赞、评论、收藏等互动数据，按照《SmartLive 热度评分体系》进行加权打分。
 * 3. 维护 ZSet 排行榜，并支持基于 30 天线性衰减的时间加成，确保新内容能够获得曝光。
 * 
 * @author smartLive
 * @date 2026-03-11
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
    
    /**
     * 增量计算并刷新热度榜单
     * 1. 从活跃队列中提取近期有互动（点赞/收藏等）的博客。
     * 2. 融合当前榜单前 N 名，确保头部数据的竞争实时性。
     * 3. 重新计算分数并更新 Redis ZSet。
     */
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

    /**
     * 全量重建博客榜单任务
     * 通常在系统初始化或算法大幅调整时执行，遍历所有有效博客 ID 进行重建。
     */
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
     * 博客热度打分公式（参见《SmartLive 热度评分体系设计》第四章）
     *
     * hotScore = baseScore + liked × 0.4 + commentCount × 0.3 + stared × 0.2 + timeDecay × 0.1
     * 初始分：普通用户 = 50, 达人/认证用户 = 65
     * timeDecay = max(0, 30 - 发布天数)
     *
     * @param blog 博客信息
     * @return 热度分（保留两位小数）
     */
    private double calcBlogScore(BlogVO blog) {
        // 初始分：普通用户 = 50分
        double baseScore = 50.0;

        // 时间衰减：30天内的新内容有额外加成
        double timeDecay = calcTimeDecay(blog.getPublishTime());

        return roundScore(baseScore
                + safeInt(blog.getLiked()) * 0.4       // 点赞数权重最高，内容质量核心指标
                + safeInt(blog.getComments()) * 0.3    // 评论数，反映互动活跃度
                + safeInt(blog.getStared()) * 0.2      // 收藏数，长期价值体现
                + timeDecay * 0.1                      // 时间衰减，30天内新内容加成
        );
    }
}
