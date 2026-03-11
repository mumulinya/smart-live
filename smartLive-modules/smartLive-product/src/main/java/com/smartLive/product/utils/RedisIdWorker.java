package com.smartLive.product.utils;

import com.smartLive.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 基于 Redis 的全局唯一 ID 生成器
 * 实现原理：时间戳(31位) + 序列号(32位)
 * 能够保证在分布式环境下的唯一性、递增性及高性能
 */
@Component
public class RedisIdWorker {

    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS=32;
    @Autowired
    private RedisService redisService;

    /**
     * 获取下一个全局唯一 ID
     * 
     * @param keyPrefix 业务前缀（如 order, blog 等）
     * @return 64位长整型 ID
     */
    public Long nextId(String keyPrefix){
        // 1. 生成时间戳：当前秒数 - 项目起始时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号：根据业务前缀与日期进行增量统计
        // 精确到天，方便后期统计订单量或进行 Key 过期管理
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 利用 Redis 的原子自增保证 ID 在同一秒内的唯一性
        long count = redisService.incrementCacheValue("icr:" + keyPrefix + ":" + date);

        // 3. 拼接并返回：左移序列号位数后进行或运算
        return timestamp << COUNT_BITS | count;
    }
}
