package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.interaction.domain.VO.ProductVO;
import com.smartLive.product.api.RemoteProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品（代金券与团购套餐）热度计算策略实现
 * 
 * 核心逻辑：
 * 1. 维护“代金券榜”与“团购套餐榜”两个独立的双榜单。
 * 2. 初始分：对秒杀活动商品（Seckill）给予更高的冷启动初始分。
 * 3. 权重设计：以“已售数量（Sold）”为核心指标，辅以收藏、评价等反馈数据。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Component
public class ProductHotRankStrategyImpl extends AbstractHotRankStrategy {

    @Autowired
    private RemoteProductService remoteProductService;
    
    @Override
    public Integer getType() { 
        return GlobalBizTypeEnum.PRODUCT.getCode(); 
    }
    
    /**
     * 增量刷新商品热度榜单
     * 同时支持代金券（Voucher）与团购（Deal）两类榜单的同步维护。
     */
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
            
            List<ProductVO> productList = getResourceList(getType(), new ArrayList<>(candidateIds));
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
                
                double score = calcProductScore(product);
                
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

    /**
     * 全量重建所有商品的热榜数据
     */
    @Override
    public void fullRebuildRank() {
        log.info("开始全量重建商品热榜...");
        String voucherRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY + "voucher";
        String dealRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY + "deal";
        try {
            // 1. 获取全部商品 ID
            List<Long> allIds = remoteProductService.getAllProductIds();
            if (CollUtil.isEmpty(allIds)) {
                log.warn("全量重建商品热榜：未获取到任何商品ID");
                return;
            }
            log.info("全量重建商品热榜：共获取到 {} 个商品ID", allIds.size());

            // 2. 分批获取商品详情并计算分数
            Set<ZSetOperations.TypedTuple<String>> voucherTuples = new HashSet<>();
            Set<ZSetOperations.TypedTuple<String>> dealTuples = new HashSet<>();
            List<List<Long>> partitions = ListUtil.partition(allIds, 200);
            for (List<Long> batch : partitions) {
                List<ProductVO> productList = getResourceList(getType(), batch);
                if (CollUtil.isEmpty(productList)) continue;

                for (ProductVO product : productList) {
                    if (product == null || product.getId() == null) continue;
                    // 跳过已下线商品
                    if (product.getStatus() != null && product.getStatus() == 2) continue;

                    double score = calcProductScore(product);
                    if (product.getCategory() != null && product.getCategory() == 1) {
                        voucherTuples.add(new DefaultTypedTuple<>(String.valueOf(product.getId()), score));
                    } else {
                        dealTuples.add(new DefaultTypedTuple<>(String.valueOf(product.getId()), score));
                    }
                }
            }

            // 3. 原子性替换
            if (!voucherTuples.isEmpty()) {
                redisService.deleteObject(voucherRankKey);
                redisService.setCacheZSet(voucherRankKey, voucherTuples);
                log.info("全量重建商品代金券热榜完成：共写入 {} 条数据", voucherTuples.size());
            }
            if (!dealTuples.isEmpty()) {
                redisService.deleteObject(dealRankKey);
                redisService.setCacheZSet(dealRankKey, dealTuples);
                log.info("全量重建商品团购热榜完成：共写入 {} 条数据", dealTuples.size());
            }
        } catch (Exception e) {
            log.error("全量重建商品热榜异常", e);
        }
    }

    /**
     * 商品热度打分公式（参见《SmartLive 热度评分体系设计》第三章）
     *
     * hotScore = baseScore + sold × 0.4 + stars × 0.3 + reviewCount × 0.2 + fans × 0.1
     * 初始分：普通商品 = 60, 秒杀商品 = 80
     *
     * @param product 商品信息
     * @return 热度分（保留两位小数）
     */
    private double calcProductScore(ProductVO product) {
        // 初始分：秒杀活动商品 = 80分，普通商品 = 60分
        double baseScore = (product.getActivityType() != null && product.getActivityType() == 1)
                ? 80.0 : 60.0;

        return roundScore(baseScore
                + safeInt(product.getSold()) * 0.4      // 销量权重最高，商品以销量为核心
                + safeInt(product.getStars()) * 0.3     // 收藏数
                + safeInt(product.getReviews()) * 0.2   // 评价数量
                + safeInt(product.getFans()) * 0.1      // 关注数
        );
    }
}
