package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.domain.VO.BlogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 博客本身的人气榜，按收藏/点赞独立计算 
 */
@Slf4j
@Component
public class BlogHotRankStrategyImpl extends AbstractHotRankStrategy {
    
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

            // 获取到所有需要重算的 Blog ID
            Set<Long> candidateIds = toLongSet(activeSet);
            String hotRankKey = RedisConstants.BLOG_HOT_RANK_KEY;
            
            // 包含原先榜单前 N 名一起重算时间衰减
            candidateIds.addAll(getTopIds(hotRankKey, HOT_RANK_MERGE_TOP_N));
            
            List<BlogVO> blogList = getResourceList(getType(), new java.util.ArrayList<>(candidateIds));
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
                
                // 博客热度打分公式
                double likeWeight = 1.5D;
                double commentWeight = 2.0D;
                
                double interaction = safeInt(blog.getLiked()) * likeWeight + safeInt(blog.getComments()) * commentWeight + 1.0D;
                double score = applyTimeDecay(interaction, blog.getPublishTime() == null ? null : blog.getPublishTime().getTime());
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
}
