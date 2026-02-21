package com.smartLive.common.redis.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.smartLive.common.core.constant.RedisData;
import com.smartLive.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RedisMultiCacheManager {

    private static final String REDIS_DATA_FIELD = "data";
    private static final String REDIS_EXPIRE_TIME_FIELD = "expireTime";

    @Autowired
    private RedisService redisService;

    /**
     * Generic batch cache query utility.
     */
    public <T> List<T> queryBatchWithCache(
            String keyPrefix,
            List<Long> ids,
            Class<T> clazz,
            Function<List<Long>, List<T>> dbQueryFn,
            Function<T, Long> idExtractor,
            long expireTime,
            TimeUnit timeUnit) {

        if (ids == null || ids.isEmpty()) {
            log.debug("[queryBatchWithCache] ids is empty");
            return Collections.emptyList();
        }

        List<Long> distinctIds = ids.stream().distinct().collect(Collectors.toList());
        List<String> keys = distinctIds.stream().map(id -> keyPrefix + id).collect(Collectors.toList());

        List<Object> redisResults = redisService.getMultiCacheObject(keys);
        List<T> finalResult = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();

        for (int i = 0; i < distinctIds.size(); i++) {
            Object cacheObj = redisResults != null ? redisResults.get(i) : null;
            CacheReadResult<T> readResult = readCacheValue(cacheObj, clazz);

            if (readResult.getState() == CacheState.HIT) {
                finalResult.add(readResult.getData());
            } else if (readResult.getState() == CacheState.MISS) {
                missingIds.add(distinctIds.get(i));
            }
            // NULL_PLACEHOLDER: skip and do not query DB
        }

        if (!missingIds.isEmpty()) {
            List<T> dbResults = dbQueryFn.apply(missingIds);

            if (dbResults != null && !dbResults.isEmpty()) {
                finalResult.addAll(dbResults);

                Map<String, Object> cacheMap = new HashMap<>();
                LocalDateTime logicalExpireTime = LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime));
                for (T item : dbResults) {
                    Long itemId = idExtractor.apply(item);
                    RedisData redisData = new RedisData();
                    redisData.setData(item);
                    redisData.setExpireTime(logicalExpireTime);
                    cacheMap.put(keyPrefix + itemId, JSONUtil.toJsonStr(redisData));
                }

                redisService.setMultiCacheObject(cacheMap);

                int randomJitter = new Random().nextInt(60);
                for (String key : cacheMap.keySet()) {
                    redisService.expire(key, expireTime + randomJitter, timeUnit);
                }
            }
        }

        log.debug("[queryBatchWithCache] finalResult.size: {}", finalResult.size());
        return finalResult;
    }

    private <T> CacheReadResult<T> readCacheValue(Object cacheObj, Class<T> clazz) {
        if (cacheObj == null) {
            return CacheReadResult.miss();
        }

        String jsonStr = cacheObj.toString();
        if (StrUtil.isBlank(jsonStr)) {
            return CacheReadResult.nullPlaceholder();
        }

        try {
            JSONObject jsonObject = JSONUtil.parseObj(jsonStr);
            if (jsonObject.containsKey(REDIS_DATA_FIELD) && jsonObject.containsKey(REDIS_EXPIRE_TIME_FIELD)) {
                RedisData redisData = JSONUtil.toBean(jsonObject, RedisData.class);
                LocalDateTime expireTime = redisData.getExpireTime();
                if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
                    return CacheReadResult.miss();
                }

                Object dataObj = redisData.getData();
                if (dataObj == null) {
                    return CacheReadResult.nullPlaceholder();
                }

                T data;
                if (dataObj instanceof JSONObject) {
                    data = JSONUtil.toBean((JSONObject) dataObj, clazz);
                } else {
                    data = JSONUtil.toBean(JSONUtil.toJsonStr(dataObj), clazz);
                }
                return CacheReadResult.hit(data);
            }
        } catch (Exception e) {
            // Fall back to old plain-JSON parsing.
        }

        try {
            T oldFormatData = JSON.parseObject(jsonStr, clazz);
            if (oldFormatData == null) {
                return CacheReadResult.nullPlaceholder();
            }
            return CacheReadResult.hit(oldFormatData);
        } catch (Exception e) {
            log.warn("Failed to parse batch cache json: {}", jsonStr, e);
            return CacheReadResult.miss();
        }
    }

    private enum CacheState {
        HIT,
        MISS,
        NULL_PLACEHOLDER
    }

    private static class CacheReadResult<T> {
        private final CacheState state;
        private final T data;

        private CacheReadResult(CacheState state, T data) {
            this.state = state;
            this.data = data;
        }

        private static <T> CacheReadResult<T> hit(T data) {
            return new CacheReadResult<>(CacheState.HIT, data);
        }

        private static <T> CacheReadResult<T> miss() {
            return new CacheReadResult<>(CacheState.MISS, null);
        }

        private static <T> CacheReadResult<T> nullPlaceholder() {
            return new CacheReadResult<>(CacheState.NULL_PLACEHOLDER, null);
        }

        private CacheState getState() {
            return state;
        }

        private T getData() {
            return data;
        }
    }
}
