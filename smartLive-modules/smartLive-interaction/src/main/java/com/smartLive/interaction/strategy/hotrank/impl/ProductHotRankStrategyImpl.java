package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.domain.VO.ProductVO;
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
 * 商品本身（代金券和团购套餐）的人气榜 
 */
@Slf4j
@Component
public class ProductHotRankStrategyImpl extends AbstractHotRankStrategy {
    
    @Override
    public Integer getType() { 
        return GlobalBizTypeEnum.PRODUCT.getCode(); 
    }
    
    @Override
    public void calculateAndRefreshRank() {
        String calcKey = RedisConstants.PRODUCT_CALC_QUEUE_KEY;
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
            // 商品分两榜：代金券榜与团购榜
            String voucherRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY + "voucher";
            String dealRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY + "deal";
            
            candidateIds.addAll(getTopIds(voucherRankKey, HOT_RANK_MERGE_TOP_N));
            candidateIds.addAll(getTopIds(dealRankKey, HOT_RANK_MERGE_TOP_N));
            
            List<ProductVO> productList = getResourceList(getType(), new java.util.ArrayList<>(candidateIds));
            if (CollUtil.isEmpty(productList)) {
                redisService.deleteObject(tempKey);
                return;
            }

            Map<Long, ProductVO> productMap = productList.stream().collect(Collectors.toMap(ProductVO::getId, p -> p, (k1, k2) -> k1));
            Set<ZSetOperations.TypedTuple<String>> voucherTuples = new HashSet<>();
            Set<ZSetOperations.TypedTuple<String>> dealTuples = new HashSet<>();
            
            for (Long id : candidateIds) {
                ProductVO product = productMap.get(id);
                boolean isVoucher = false;
                String hotRankKey = dealRankKey;

                // product.getStatus() == 2 代表已下线的情况
                if (product == null || (product.getStatus() != null && product.getStatus() == 2)) {
                    redisService.removeCacheZSetObject(voucherRankKey, String.valueOf(id));
                    redisService.removeCacheZSetObject(dealRankKey, String.valueOf(id));
                    continue;
                }
                
                // 商品类别 1:代金券, 2:团购套餐
                if (product.getCategory() != null && product.getCategory() == 1) {
                    isVoucher = true;
                    hotRankKey = voucherRankKey;
                }
                
                // 根据产品的价格做非常基础的分数差异化，因为目前只有价格与库存可以对比
                double interaction = 10.0D; 
                if (product.getPrice() != null) {
                    interaction += Math.log(product.getPrice().doubleValue() + 1); // 极其简单的模型替代
                }
                double score = applyTimeDecay(interaction, null); // 商品暂无上架时间，永不衰减
                
                if (isVoucher) {
                    voucherTuples.add(new DefaultTypedTuple<>(String.valueOf(id), score));
                } else {
                    dealTuples.add(new DefaultTypedTuple<>(String.valueOf(id), score));
                }
            }
            
            if (!voucherTuples.isEmpty()) {
                redisService.setCacheZSet(voucherRankKey, voucherTuples);
            }
            if (!dealTuples.isEmpty()) {
                redisService.setCacheZSet(dealRankKey, dealTuples);
            }
            redisService.deleteObject(tempKey);
        } catch (Exception e) {
            log.error("计算商品热度榜异常", e);
        }
    }
}
