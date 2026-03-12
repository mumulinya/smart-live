package com.smartLive.common.redis.util;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.RedisData;
import com.smartLive.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redis 多级缓存管理器
 * 
 * 本类旨在提供一套标准化的、高性能的缓存访问方案：
 * 1. 逻辑对齐 CacheClient 模式。
 * 2. 采用【逻辑过期】解决批量查询场景下的缓存击穿问题。
 * 3. 采用【空值占位符】解决缓存穿透问题。
 * 4. 支持异步缓存重建，保证 C 端请求极高响应速度的基础上实现最终一致性。
 */
@Component
@Slf4j
public class RedisMultiCacheManager {

    /**
     * 专属缓存重建线程池
     * 采用固定大小线程池，避免高并发下创建过多线程消耗系统资源。
     * 所有异步重建逻辑（逻辑过期触发）均通过此池执行，不阻塞主业务请求线程。
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Autowired
    private RedisService redisService;

    /**
     * 设置逻辑过期时间 (数据格式完全对齐 CacheClient)
     * 
     * @param key   缓存 Key
     * @param value 存入的数据对象
     * @param time  逻辑过期的时间值
     * @param unit  逻辑过期的时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        // 1. 构造包装对象：包含原始数据和预期的逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        // 逻辑过期时间 = 当前时间 + 指定步长
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        
        // 2. 写入 Redis
        // 注意：物理过期时间统一由 RedisConstants.CACHE_LOGICAL_EXPIRE_TTL 管理（通常为1天），
        // 这样可以确保数据在逻辑过期后依然存在于 Redis 中，方便旧数据的瞬间返回和后台异步更新。
        redisService.setCacheObject(key, JSONUtil.toJsonStr(redisData), RedisConstants.CACHE_LOGICAL_EXPIRE_TTL, TimeUnit.DAYS);
    }

    /**
     * 【核心读链路】批量查询缓存 - 采用逻辑过期解决缓存击穿
     *
     * @param keyPrefix      缓存 Key 的前缀 (例如: "cache:prod:")
     * @param lockKeyPrefix   分布式锁的前缀 (例如: "lock:prod:")
     * @param ids            待查询的任务 ID 列表 (例如: [1, 2, 3])
     * @param clazz          返回数据实体的 Class 类型
     * @param dbQueryFn      数据库回源函数：当缓存缺失或过期时，如何从 DB 获取数据
     * @param idExtractor    ID 提取函数：从实体对象中提取 ID，用于结果集回填和存入缓存
     * @param expireTime     逻辑过期的时间步长
     * @param timeUnit       逻辑过期的时间单位
     * @param <T>            数据实体泛型
     * @return 组装好的实体列表
     */
    public <T> List<T> queryBatchWithCache(
            String keyPrefix,
            String lockKeyPrefix,
            List<Long> ids,
            Class<T> clazz,
            Function<List<Long>, List<T>> dbQueryFn,
            Function<T, Long> idExtractor,
            long expireTime,
            TimeUnit timeUnit) {

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. ID 去重，避免重复查询相同 Key
        List<Long> distinctIds = ids.stream().distinct().toList();
        // 2. 构造缓存 Keys 列表
        List<String> keys = distinctIds.stream().map(id -> keyPrefix + id).collect(Collectors.toList());

        // 3. 批量从 Redis 获取数据 (MGET)，大幅减少网络 RTT 往返开销
        List<Object> redisResults = redisService.getMultiCacheObject(keys);
        
        List<T> finalResult = new ArrayList<>();        // 最终返回结果集
        List<Long> missingIds = new ArrayList<>();       // 完全未命中的 ID (Cache MISS)
        List<Long> logicalExpiredIds = new ArrayList<>(); // 逻辑上已过期的 ID (LOGICAL_EXPIRE)

        // 4. 解析 Redis 返回的原始数据结果，对状态进行分类
        for (int i = 0; i < distinctIds.size(); i++) {
            Long currentId = distinctIds.get(i);
            Object cacheObj = redisResults != null ? redisResults.get(i) : null;
            CacheReadResult<T> readResult = readCacheValue(cacheObj, clazz);

            switch (readResult.getState()) {
                case HIT:
                    // 状态：击中且有效。直接加入结果。
                    finalResult.add(readResult.getData());
                    break;
                case LOGICAL_EXPIRE:
                    // 状态：击中旧数据但逻辑上已过期。
                    // 返回旧数据给用户，并标记该 ID 需要异步发起更新重建。
                    finalResult.add(readResult.getData());
                    logicalExpiredIds.add(currentId);
                    break;
                case MISS:
                    // 状态：Redis 中完全无此 key 数据。需要同步回 DB。
                    missingIds.add(currentId);
                    break;
                case NULL_PLACEHOLDER:
                    // 状态：击中空值占位符（即之前查询过且确实无此数据）。
                    // 直接跳过，防止频繁回源导致数据库压力过大（解决缓存穿透）。
                    break;
            }
        }

        // 5. 【异步轨】：处理逻辑过期的数据重建
        if (!logicalExpiredIds.isEmpty()) {
            for (Long id : logicalExpiredIds) {
                String lockKey = lockKeyPrefix + id;
                String cacheKey = keyPrefix + id;
                
                // 尝试获取分布式锁（防止多个线程同时发起同一个 ID 的重建请求）
                if (tryLock(lockKey)) {
                    CACHE_REBUILD_EXECUTOR.submit(() -> {
                        try {
                            // 拿到锁后，单独启动线程从 DB 查询最新值
                            List<T> rebuildResults = dbQueryFn.apply(Collections.singletonList(id));
                            if (rebuildResults == null || rebuildResults.isEmpty()) {
                                // 如果 DB 依然查不到，存入空字符串占位，避免下次再回源
                                redisService.setCacheObject(cacheKey, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                            } else {
                                T r1 = rebuildResults.get(0);
                                // 回写 Redis 并更新逻辑过期时间
                                this.setWithLogicalExpire(cacheKey, r1, expireTime, timeUnit);
                                log.info("批量任务中异步动态更新缓存成功: {}", cacheKey);
                            }
                        } catch (Exception e) {
                            log.error("逻辑过期后台重建失败 [Key={}]: {}", cacheKey, e.getMessage());
                        } finally {
                            // 最终释放分布式锁
                            unLock(lockKey);
                        }
                    });
                }
            }
        }

        // 6. 【同步轨】：处理完全缺失的数据 (同步回 DB 补全)
        if (!missingIds.isEmpty()) {
            List<T> dbResults = dbQueryFn.apply(missingIds);
            if (dbResults != null && !dbResults.isEmpty()) {
                finalResult.addAll(dbResults);
                // 批量回源的数据，每一项都要独立存入 Redis
                for (T item : dbResults) {
                    this.setWithLogicalExpire(keyPrefix + idExtractor.apply(item), item, expireTime, timeUnit);
                }
                
                // 7. 处理【部分未击中】：即在 missingIds 中，DB 依然没查到的那些记录
                Set<Long> foundIds = dbResults.stream().map(idExtractor).collect(Collectors.toSet());
                for (Long id : missingIds) {
                    if (!foundIds.contains(id)) {
                        // 依然查无此数据，标记空占位，防止缓存穿透
                        redisService.setCacheObject(keyPrefix + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                    }
                }
            } else {
                // 如果整批同步回源结果全为空，全部设为占位符
                for (Long id : missingIds) {
                    redisService.setCacheObject(keyPrefix + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                }
            }
        }

        return finalResult;
    }

    /**
     * 【解析逻辑】对 Redis 读出的原始对象进行二次加工解析
     * (底层逻辑完全同步自 CacheClient: readCacheValue)
     *
     * @param cacheObj Redis MGET 出来的原始对象 (可能是反序列化后的 JSONObject 或 String)
     * @param clazz    目标业务实体类
     * @return 包含状态枚举和数据的复合包装类
     */
    private <T> CacheReadResult<T> readCacheValue(Object cacheObj, Class<T> clazz) {
        // 情况 A：完全无缓存数据
        if (cacheObj == null) {
            return CacheReadResult.miss();
        }

        String json = cacheObj.toString();
        // 情况 B：空值占位符处理 (存储的是 "")
        if (json.isEmpty()) {
            return CacheReadResult.nullPlaceholder();
        }
        
        // 情况 C：纯空白串处理，视为 MISS
        if (StrUtil.isBlank(json)) {
            return CacheReadResult.miss();
        }

        try {
            // 解析包装类 RedisData
            RedisData redisData = JSONUtil.toBean(json, RedisData.class);
            Object dataObj = redisData.getData();
            
            // 内部存储的数据对象若不存在，亦视为占位处理
            if (dataObj == null) {
                return CacheReadResult.nullPlaceholder();
            }

            // 二次转换：将 JSON 格式的数据对象恢复为具体的业务类型 clazz
            T data;
            if (dataObj instanceof JSONObject) {
                data = JSONUtil.toBean((JSONObject) dataObj, clazz);
            } else {
                // 兼容基本类型或非对象结构的解析
                data = JSONUtil.toBean(JSONUtil.toJsonStr(dataObj), clazz);
            }

            // 核心判定：是否属于逻辑过期？
            if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
                // 当前时间未到过期时间 -> 正常命中
                return CacheReadResult.hit(data);
            } else {
                // 当前时间已超逻辑过期时间 -> 过期命中 (触发异步刷新)
                return CacheReadResult.logicalExpire(data);
            }
        } catch (Exception e) {
            // 解析异常时（如数据格式不兼容），退回 MISS 状态并强制由 DB 重刷
            log.warn("缓存数据反序列化异常，已强制退回 MISS 状态: {}", e.getMessage());
            return CacheReadResult.miss();
        }
    }

    /**
     * 【互斥锁】尝试获取分布式锁
     * 采用 setnx 指令实现基元锁，锁过期时间默认采用 RedisConstants.LOCK_SHOP_TTL
     */
    private boolean tryLock(String key) {
        Boolean flag = redisService.setCacheObjectIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 【互斥锁】手动释放分布式锁
     */
    private void unLock(String key) {
        redisService.deleteObject(key);
    }

    /**
     * 内部缓存状态枚举
     */
    private enum CacheState {
        HIT,               // 命中且有效
        LOGICAL_EXPIRE,    // 命中但逻辑过期
        MISS,             // 未命中
        NULL_PLACEHOLDER  // 命中了防穿透的空值占位符
    }

    /**
     * 内部解析结果包装类
     */
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

        private static <T> CacheReadResult<T> logicalExpire(T data) {
            return new CacheReadResult<>(CacheState.LOGICAL_EXPIRE, data);
        }

        private static <T> CacheReadResult<T> miss() {
            return new CacheReadResult<>(CacheState.MISS, null);
        }

        private static <T> CacheReadResult<T> nullPlaceholder() {
            return new CacheReadResult<>(CacheState.NULL_PLACEHOLDER, null);
        }

        private CacheState getState() { return state; }
        private T getData() { return data; }
    }
}
