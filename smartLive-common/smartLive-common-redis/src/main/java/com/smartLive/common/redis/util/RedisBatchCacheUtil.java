package com.smartLive.common.redis.util;

import com.alibaba.fastjson2.JSON;
import com.smartLive.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RedisBatchCacheUtil {

    @Autowired
    private RedisService redisService;

    /**
     * 通用的批量缓存查询方法
     *
     * @param keyPrefix     Redis Key 的前缀 (例如: "shop:detail:")
     * @param ids           要查询的 ID 列表
     * @param dbQueryFn     查数据库的函数 (当缓存未命中时，靠它去查库)
     * @param idExtractor   ID 提取函数 (用来把查到的对象重新映射回 Redis Key)
     * @param expireTime    过期时间
     * @param timeUnit      时间单位
     * @param <>          ID 的类型 (比如 Long)
     * @param <T>           返回对象的类型 (比如 ShopDTO)
     */
    public <T> List<T> queryBatchWithCache(
            String keyPrefix,
            List<Long> ids,
            Class<T> clazz, // 👈 新增：必须传入目标类型，例如 ShopVO.class，用来做 JSON 反序列化
            Function<List<Long>, List<T>> dbQueryFn,
            Function<T, Long> idExtractor,
            long expireTime,
            TimeUnit timeUnit) {

        if (ids == null || ids.isEmpty()) {
            log.debug("[queryBatchWithCache] ids is empty");
            return Collections.emptyList();
        }

        // 1. 去重并构造 Redis Keys
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<String> keys = distinctIds.stream().map(id -> keyPrefix + id).collect(Collectors.toList());

        // 2. 批量查 Redis
        List<Object> redisResults = redisService.getMultiCacheObject(keys);
        List<T> finalResult = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();

        // 3. 分拣命中与未命中的数据
        for (int i = 0; i < distinctIds.size(); i++) {
            Object obj = redisResults != null ? redisResults.get(i) : null;
            if (obj != null) {
                // ✅ 核心改动 1：从 Redis 拿出来的是 Object (通常是 String)，统一转成 String
                String jsonStr = obj.toString();
                // ✅ 核心改动 2：将 JSON 字符串安全地反序列化为目标对象 T
                T item = JSON.parseObject(jsonStr, clazz);
                finalResult.add(item);
            } else {
                missingIds.add(distinctIds.get(i));
            }
        }

        // 4. 处理未命中的数据（回调查库逻辑）
        if (!missingIds.isEmpty()) {
            // 调用外部传入的查库方法
            List<T> dbResults = dbQueryFn.apply(missingIds);

            if (dbResults != null && !dbResults.isEmpty()) {
                finalResult.addAll(dbResults);

                // 5. 组装数据，准备写回 Redis
                Map<String, Object> cacheMap = new HashMap<>();
                for (T item : dbResults) {
                    Long itemId = idExtractor.apply(item);
                    // ✅ 核心改动 3：将 Java 对象序列化为 JSON 字符串后，再存入 Redis
                    String jsonString = JSON.toJSONString(item);
                    cacheMap.put(keyPrefix + itemId, jsonString);
                }

                // 批量写入 Redis
                redisService.setMultiCacheObject(cacheMap);

                // 设置带有随机抖动的过期时间 (防雪崩)
                int randomJitter = new Random().nextInt(60);
                for (String key : cacheMap.keySet()) {
                    redisService.expire(key, expireTime + randomJitter, timeUnit);
                }
            }
        }
        log.debug("[queryBatchWithCache] finalResult.size: {}", finalResult.size());
        return finalResult;
    }
}