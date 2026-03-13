package com.smartLive.interaction.strategy.hotrank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
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
 * 店铺热度计算策略实现
 * 
 * 核心指标：
 * 1. 销量（Sold）：直接反映店铺的经营热度。
 * 2. 评分（Score）：体现商户的口碑与服务质量，权重最高。
 * 3. 互动数：包含评价数与收藏数。
 * 注意：店铺作为长期经营实体，不设时间衰减项，完全由市场表现决定排名。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Component
public class ShopHotRankStrategyImpl extends AbstractHotRankStrategy {

    @Autowired
    private RemoteShopService remoteShopService;

    /**
     * 获取当前策略对应的业务类型。
     *
     * @return 业务类型编码，此处为店铺类型。
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.SHOP.getCode();
    }

    /**
     * 同步刷新有变动的店铺热度分。
     * 该方法会从 Redis 的计算队列中取出待更新的店铺ID，
     * 合并当前热榜中的部分高排名店铺，然后批量获取这些店铺的最新数据，
     * 计算新的热度分数并更新到 Redis 热榜中。
     */
    @Override
    public void calculateAndRefreshRank() {
        String calcKey = RedisConstants.SHOP_CALC_QUEUE_KEY;
        String tempKey = calcKey + ":TEMP";
        try {
            // 如果计算队列为空，则直接返回
            if (Boolean.FALSE.equals(redisService.hasKey(calcKey))) return;

            // 将计算队列重命名为临时队列，确保原子性处理
            redisService.rename(calcKey, tempKey);
            // 获取临时队列中的待更新店铺ID集合
            Set<Object> activeSet = redisService.getCacheSet(tempKey);
            if (CollUtil.isEmpty(activeSet)) {
                redisService.deleteObject(tempKey);
                return;
            }

            // 将待更新ID转换为Long类型集合
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
     * 店铺热度打分公式（参见《SmartLive 热度评分体系设计》第二章）
     *
     * hotScore = baseScore + sold × 0.3 + score × 0.4 + commentCount × 0.2 + stars × 0.1
     *
     * @param shop 店铺信息
     * @return 热度分（保留两位小数）
     */
    private double calcShopScore(ShopVO shop) {
        // 初始分：普通新店铺 = 60分
        double baseScore = 60.0;

        // 评分字段 score 是 1~5 分，乘10 保存（如 45 代表 4.5 分）
        double score = safeInt(shop.getScore()) / 10.0;

        return roundScore(baseScore
                + safeInt(shop.getSold()) * 0.3     // 销量，反映受欢迎程度
                + score * 0.4                        // 评分权重最高，口碑最重要
                + safeInt(shop.getComments()) * 0.2   // 评价数量，反映活跃度
                + safeInt(shop.getStars()) * 0.1      // 收藏数，社交认可度
        );
    }
}
