package com.smartLive.interaction.tool;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 获取redis中存储的源工具类
 * @author huangjian
 * @date 2018年5月17日
 */
@Component
@RequiredArgsConstructor
public class QueryRedisSourceIdsTool {

    private  final StringRedisTemplate stringRedisTemplate;    /**
     * 通用方法：从 Redis ZSet 中分页查询 ID 列表
     * @param keyPrefix Redis Key 前缀 (如 "follow:user:")
     * @param userId    用户 ID
     * @param page      当前页
     * @param size      每页条数
     * @return Page<Long> 包含 total 和 ID列表
     */
   public  Page<Long> queryRedisIdPage(String keyPrefix, Long userId, long page, long size) {
        String key = keyPrefix + userId;

        // 1. 查总数 (ZCARD)
        Long total = stringRedisTemplate.opsForZSet().zCard(key);
        if (total == null || total == 0) {
            return new Page<>(page, size, 0); // 返回空页
        }

        // 2. 计算下标 (ZREVRANGE start stop)
        long start = (page - 1) * size;
        long end = start + size - 1;

        // 3. 查 ID 集合 (按分数倒序，即时间倒序)
        Set<String> idStrSet = stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
        if (CollUtil.isEmpty(idStrSet)) {
            return new Page<>(page, size, total);
        }

        // 4. 类型转换 String -> Long
        List<Long> idList = idStrSet.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 5. 封装成 Page 对象返回
        Page<Long> idPage = new Page<>(page, size);
        idPage.setTotal(total);
        idPage.setRecords(idList);

        return idPage;
    }
    /**
     * 获取共同关注列表
     *
     * @param userId1
     * @param userId2
     * @param
     * @return
     */
    public  Page<Long> queryRedisCommonFollowIdPage(String FollowKeyPrefix, Long userId1, Long userId2, long page, long size) {
        String key1 = FollowKeyPrefix+ userId1;
        String key2 = FollowKeyPrefix + userId2;

        // 1. 定义一个临时的目标 Key
        // 建议加上 distinct 前缀，避免冲突
        String destKey = "temp:common:" +FollowKeyPrefix+":"+userId1 + ":" + userId2;

        // 2. 【核心】计算交集并存储到 destKey，返回交集的大小 (Total)
        // 对应 Redis 命令: ZINTERSTORE destKey 2 key1 key2
        Long total = stringRedisTemplate.opsForZSet().intersectAndStore(key1, key2, destKey);

        if (total == null || total == 0) {
            return new Page<>(page, size, 0); // 返回空页
        }

        // 3. 【必须】设置过期时间 (比如 60 秒后自动删除)
        // 因为共同关注是会变的，而且在这个 Key 只是为了临时分页用
        stringRedisTemplate.expire(destKey, 60, TimeUnit.SECONDS);

        // 4. 标准的分页查询逻辑 (从临时 Key 里查)
        long start = (page - 1) * size;
        long end = start + size - 1;

        // 按分数倒序取 (ZSet 交集默认是将两个元素的分数相加，通常这能反映"两人都比较晚关注"的权重)
        Set<String> idStrSet = stringRedisTemplate.opsForZSet().reverseRange(destKey, start, end);

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

        return idPage;
    }
}
