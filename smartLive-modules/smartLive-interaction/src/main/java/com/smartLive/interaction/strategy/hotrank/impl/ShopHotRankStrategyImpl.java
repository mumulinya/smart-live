package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.domain.VO.ShopVO;
import com.smartLive.shop.api.RemoteShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** 
 * 店铺本身的人气榜 
 */
@Slf4j
@Component
public class ShopHotRankStrategyImpl extends AbstractHotRankStrategy {

    @Autowired
    private RemoteShopService remoteShopService;
    
    @Override
    public Integer getType() { 
        return GlobalBizTypeEnum.SHOP.getCode(); 
    }
    
    @Override
    public void calculateAndRefreshRank() {
        String calcKey = RedisConstants.SHOP_CALC_QUEUE_KEY;
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
            String hotRankKey = RedisConstants.SHOP_HOT_RANK_KEY;
            candidateIds.addAll(getTopIds(hotRankKey, HOT_RANK_MERGE_TOP_N));
            
            List<ShopVO> shopList = getResourceList(getType(), new ArrayList<>(candidateIds));
            if (CollUtil.isEmpty(shopList)) {
                redisService.deleteObject(tempKey);
                return;
            }

            Map<Long, ShopVO> shopMap = shopList.stream().collect(Collectors.toMap(ShopVO::getId, s -> s, (k1, k2) -> k1));
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
            
            for (Long id : candidateIds) {
                ShopVO shop = shopMap.get(id);
                if (shop == null) {
                    redisService.removeCacheZSetObject(hotRankKey, String.valueOf(id));
                    continue;
                }
                
                double score = calcShopScore(shop);
                tuples.add(new DefaultTypedTuple<>(String.valueOf(id), score));
            }
            
            if (!tuples.isEmpty()) {
                redisService.setCacheZSet(hotRankKey, tuples);
            }
            redisService.deleteObject(tempKey);
        } catch (Exception e) {
            log.error("计算店铺热度榜异常", e);
        }
    }

    @Override
    public void fullRebuildRank() {
        log.info("开始全量重建店铺热榜...");
        String hotRankKey = RedisConstants.SHOP_HOT_RANK_KEY;
        try {
            // 1. 获取全部店铺 ID
            List<Long> allIds = remoteShopService.getAllShopIds();
            if (CollUtil.isEmpty(allIds)) {
                log.warn("全量重建店铺热榜：未获取到任何店铺ID");
                return;
            }
            log.info("全量重建店铺热榜：共获取到 {} 个店铺ID", allIds.size());

            // 2. 分批获取店铺详情并计算分数
            Set<ZSetOperations.TypedTuple<String>> allTuples = new HashSet<>();
            List<List<Long>> partitions = ListUtil.partition(allIds, 200);
            for (List<Long> batch : partitions) {
                List<ShopVO> shopList = getResourceList(getType(), batch);
                if (CollUtil.isEmpty(shopList)) continue;

                for (ShopVO shop : shopList) {
                    double score = calcShopScore(shop);
                    allTuples.add(new DefaultTypedTuple<>(String.valueOf(shop.getId()), score));
                }
            }

            // 3. 原子性替换：先删旧 ZSet，再写入新数据
            if (!allTuples.isEmpty()) {
                redisService.deleteObject(hotRankKey);
                redisService.setCacheZSet(hotRankKey, allTuples);
                log.info("全量重建店铺热榜完成：共写入 {} 条数据", allTuples.size());
            }
        } catch (Exception e) {
            log.error("全量重建店铺热榜异常", e);
        }
    }

    /**
     * 店铺热度打分公式（复用于增量和全量）
     */
    private double calcShopScore(ShopVO shop) {
        double soldWeight = 2.0D;
        double commentWeight = 1.8D;
        int stars = safeInt(shop.getScore()) / 10;
        double interaction = stars * 1.5D + safeInt(shop.getSold()) * soldWeight + safeInt(shop.getComments()) * commentWeight + 1.0D;
        return applyTimeDecay(interaction, shop.getCreateTime() == null ? null : shop.getCreateTime().getTime());
    }
}
