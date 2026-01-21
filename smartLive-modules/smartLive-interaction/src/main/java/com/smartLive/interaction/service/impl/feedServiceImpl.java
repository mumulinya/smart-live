package com.smartLive.interaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.ScrollResult;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.text.Convert;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.service.IFeedService;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class feedServiceImpl implements IFeedService {
    @Autowired
    RedisService redisService;
    @Autowired
    private Map<Integer, ResourceStrategy> resourceStrategyMap;
    @Override
    public ScrollResult queryFeedList(Integer feedType, Long max, Integer offset) {
        if(UserContextHolder.getUser() == null){
            return new ScrollResult();
        }
        //获取当前登录用户
        Long userId = UserContextHolder.getUser().getId();
        FeedTypeEnum feedTypeEnum = FeedTypeEnum.getByCode(feedType);
        String key = feedTypeEnum.getFullKey(userId);

        // 2. 查 Redis (保持原有滚动分页逻辑)
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisService.getCacheZSetReverseRangeByScore(key, 0, max, offset, 5);
        if (tuples == null || tuples.isEmpty()) {
            return new ScrollResult();
        }
        long minTime = 0L;
        int os = 1;
        // 3. 解析数据
        Map<String, Map<Long,String>> idMap= new HashMap<>();
        // 时间戳
        Map<String, Map<Long, Long>> timeMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            String val = tuple.getValue().toString();
            // 解析: new:voucher:101 -> [new, voucher, 101]
            String[] parts = val.split(":");
            String type = parts[parts.length - 2];
            String action = parts.length > 2 ? parts[0] : "";
            Long id = Long.valueOf(parts[parts.length - 1]);
            if (idMap.containsKey(type)) {
                Map<Long, String> integerStringMap = idMap.get(type);
                integerStringMap.put(id,action);
                idMap.put(type, integerStringMap);
            } else {
                idMap.put(type, new HashMap<>(Map.of(id, action)));
            }
            // 滚动分页逻辑
            long time = tuple.getScore().longValue();
            if (time == minTime) os++;
            else { minTime = time; os = 1; }
            //把时间存入 timeMap，供后面使用
            timeMap.computeIfAbsent(type, k -> new HashMap<>()).put(id, time);
        }
        List<Object> voList = Lists.newArrayList();
        idMap.forEach((k, v) -> {
            Integer bizTypeEnumCode = GlobalBizTypeEnum.getByBizDomain(k).getCode();
            ResourceStrategy resourceStrategy = resourceStrategyMap.get(bizTypeEnumCode);

            // 获取动态列表
            List<Object> list = resourceStrategy.getResourceList(new ArrayList<>(v.keySet()));
            if(list != null){
                list.forEach(item -> {
                    // 设置数据类型
                    BeanUtil.setFieldValue(item,"dataType",k);
                    // 获取对象ID
                    Long id = Convert.toLong(BeanUtil.getFieldValue(item, "id"));

                    // 处理 Action
                    String action = v.get(id);
                    if(action != null && !action.isEmpty()){ // 建议加上 null 判断
                        BeanUtil.setFieldValue(item, "action", action);
                    }

                    // 【修改点 3】从 timeMap 中取出时间并赋值
                    if (timeMap.containsKey(k) && timeMap.get(k).containsKey(id)) {
                        Long timeScore = timeMap.get(k).get(id);
                        BeanUtil.setFieldValue(item, "publishTime", new Date(timeScore));
                    }
                });
                voList.addAll(list);
            }
        });
        // 6. 返回结果
        ScrollResult result = new ScrollResult();
        result.setList(voList);
        result.setMinTime(minTime);
        result.setOffset(os);
        return result;
    }
}
