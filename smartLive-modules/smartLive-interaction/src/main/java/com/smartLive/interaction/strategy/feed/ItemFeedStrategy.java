package com.smartLive.interaction.strategy.feed;

import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.smartLive.common.core.domain.ScrollResult;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ItemFeedStrategy implements FeedStrategy {
    @Autowired
    RedisService redisService;
    @Autowired
    private Map<Integer, ResourceStrategy> resourceStrategyMap;
    @Override
    public Integer getType() { return FeedTypeEnum.ITEM_FEED.getCode(); }

    /**
     * 获取动态列表 Redis Key
     *
     * @param userId
     */
    @Override
    public String getFeedKey(Long userId) {
        return FeedTypeEnum.ITEM_FEED.getFullKey(userId);
    }

    /**
     * 策略对应的方法：分页查询动态
     *
     * @param userId
     * @param max
     * @param offset
     */
    @Override
    public ScrollResult queryPage(Long userId, Long max, Integer offset) {
        String key = getFeedKey(userId);

        // 2. 查 Redis (保持原有滚动分页逻辑)
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisService.getCacheZSetReverseRangeByScore(key, 0, max, offset, 10);
        if (tuples == null || tuples.isEmpty()) {
            return new ScrollResult();
        }
        long minTime = 0L;
        int os = 1;
        Map<String, List<Long>> idMap= new HashMap<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            String val = tuple.getValue().toString();
            // 解析: new:voucher:101 -> [new, voucher, 101]
            String[] parts = val.split(":");
            String type = parts[parts.length - 2];
            Long id = Long.valueOf(parts[parts.length - 1]);
            if (idMap.containsKey(type)) {
                List<Long> longs = idMap.get(type);
                longs.add(id);
            } else {
                idMap.put(type, new ArrayList<>(Arrays.asList(id)));
            }
            // 滚动分页逻辑
            long time = tuple.getScore().longValue();
            if (time == minTime) os++;
            else { minTime = time; os = 1; }
        }
        List<Object> voList = Lists.newArrayList();
        idMap.forEach((k, v) -> {
            Integer bizTypeEnumCode = GlobalBizTypeEnum.getByBizDomain(k).getCode();
            ResourceStrategy resourceStrategy = resourceStrategyMap.get(bizTypeEnumCode);
            // 获取动态列表
            List<Object> list = resourceStrategy.getResourceList(v);
            voList.addAll(list);
        });
        // 6. 返回结果
        ScrollResult result = new ScrollResult();
        result.setList(voList);
        result.setMinTime(minTime);
        result.setOffset(os);
        return result;
    }
}