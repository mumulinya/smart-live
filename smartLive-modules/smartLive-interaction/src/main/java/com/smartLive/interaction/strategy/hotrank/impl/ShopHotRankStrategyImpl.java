package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.domain.VO.ShopVO;
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
 * 店铺本身的人气榜 
 */
@Slf4j
@Component
public class ShopHotRankStrategyImpl extends AbstractHotRankStrategy {
    
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
            
            List<ShopVO> shopList = getResourceList(getType(), new java.util.ArrayList<>(candidateIds));
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
                
                // 店铺热度打分公式
                double soldWeight = 2.0D;
                double commentWeight = 1.8D;
                int stars = safeInt(shop.getScore()) / 10; // score是10-50
                
                double interaction = stars * 1.5D + safeInt(shop.getSold()) * soldWeight + safeInt(shop.getComments()) * commentWeight + 1.0D;
                double score = applyTimeDecay(interaction, shop.getCreateTime() == null ? null : shop.getCreateTime().getTime());
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
}
