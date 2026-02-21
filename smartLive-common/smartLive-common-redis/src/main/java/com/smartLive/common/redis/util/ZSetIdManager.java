package com.smartLive.common.redis.util;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * 获取redis中存储的源工具类
 * @author mumulin
 * @date 2025年11月17日
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ZSetIdManager {
    private final RedisService redisService;

    /**
     * 将任意业务实体集合，提取 ID 和时间后，存入 Redis ZSet
     */
    public <T> void saveToZSet(String key, List<T> list, Function<T,Long> idExtractor, Function<T, Date> dateExtractor) {
        Set<ZSetOperations.TypedTuple<String>> idListSet = list.stream()
                .map(t -> {
                    // 获取id
                    String id=idExtractor.apply(t).toString();
                    // 获取时间
                    Date createTime=dateExtractor.apply(t);
                    return (ZSetOperations.TypedTuple<String>) new DefaultTypedTuple<String>(id, (double) createTime.getTime());
                })
                .collect(Collectors.toSet());
        log.info("saveToZSet:{}",idListSet);
        redisService.setCacheZSet(key, idListSet);
    }
    /**
     * 从 ZSet 中分页获取 ID 列表 (按时间/分数倒序)
     */
    public Page<Long> pageIds(String keyPrefix, Long userId, long page, long size) {
        String key = keyPrefix + userId;

        // 1. 查总数 (ZCARD)
        Long total = redisService.getCacheZSetSize(key);
        if (total == null || total == 0) {
            return new Page<>(page, size, 0); // 返回空页
        }

        // 2. 计算下标 (ZREVRANGE start stop)
        long start = (page - 1) * size;
        long end = start + size - 1;

        // 3. 查 ID 集合 (按分数倒序，即时间倒序)
        Set<Object> idStrSet = redisService.getCacheZSetReverseRange(key, start, end);
        if (CollUtil.isEmpty(idStrSet)) {
            return new Page<>(page, size, total);
        }

        // 4. 类型转换 String -> Long
        List<Long> idList = idStrSet.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(Collectors.toList());

        // 5. 封装成 Page 对象返回
        Page<Long> idPage = new Page<>(page, size);
        idPage.setTotal(total);
        idPage.setRecords(idList);
         log.info("pageIds:{}",idPage.getRecords());
        return idPage;
    }
    /**
     * 分页获取两个 ZSet 的交集 ID (常用于共同关注等场景)
     */
    public  Page<Long> pageCommonFollowIds(String FollowKeyPrefix, Long userId1, Long userId2, long page, long size) {
        String key1 = FollowKeyPrefix+ userId1;
        String key2 = FollowKeyPrefix + userId2;

        // 1. 定义一个临时的目标 Key
        // 建议加上 distinct 前缀，避免冲突
        String destKey = "temp:common:" +FollowKeyPrefix+":"+userId1 + ":" + userId2;

        // 2. 【核心】计算交集并存储到 destKey，返回交集的大小 (Total)
        // 对应 Redis 命令: ZINTERSTORE destKey 2 key1 key2
        Long total = redisService.intersectAndStoreZSet(key1, key2, destKey);
        if (total == null || total == 0) {
            return new Page<>(page, size, 0); // 返回空页
        }

        // 3. 【必须】设置过期时间 (比如 60 秒后自动删除)
        // 因为共同关注是会变的，而且在这个 Key 只是为了临时分页用
        redisService.expire(destKey, 60, TimeUnit.SECONDS);
        // 4. 标准的分页查询逻辑 (从临时 Key 里查)
        long start = (page - 1) * size;
        long end = start + size - 1;

        // 按分数倒序取 (ZSet 交集默认是将两个元素的分数相加，通常这能反映"两人都比较晚关注"的权重)
        Set<String> idStrSet = redisService.getCacheZSetReverseRange(destKey, start, end);
        if (CollUtil.isEmpty(idStrSet)) {
            return new Page<>(page, size, 0); // 返回空页
        }

        // 5. 转换 ID
        List<Long> ids = idStrSet.stream().map(Long::valueOf).collect(Collectors.toList());

        // 6. 返回 (这里只返回了 ID，如果前端要头像，后面再去调 User 服务)
        // 5. 封装成 Page 对象返回
        Page<Long> idPage = new Page<>(page, size);
        idPage.setTotal(total);
        idPage.setRecords(ids);
        log.info("pageCommonFollowIds:{}",idPage.getRecords());
        return idPage;
    }
}
