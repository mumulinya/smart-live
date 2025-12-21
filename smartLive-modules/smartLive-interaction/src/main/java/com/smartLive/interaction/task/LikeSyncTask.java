package com.smartLive.interaction.task;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.strategy.resource.ResourceFetcherStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class LikeSyncTask {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RemoteBlogService remoteBlogService; // 假设你有批量更新的 Service
    @Autowired
    private Map<String, ResourceFetcherStrategy> resourceStrategyMap;

    // 每 30 秒执行一次
    @Scheduled(cron = "0/30 * * * * ?")
    public void syncLikesToDb() {
        log.info("开始同步点赞数");
        Arrays.stream(ResourceTypeEnum.values())
                .forEach(type -> {
                    if(type.getLikedCountKeyPrefix()!=null){
                        String likedCountKeyPrefix = type.getLikedCountKeyPrefix();
                        String DIRTY_KEY = type.getLikeDirtyKeyPrefix();
                        String TEMP_KEY = DIRTY_KEY + ":TEMP";
                        // 1. 【原子操作】把脏数据 Key 重命名为临时 Key
                        // 这样新的点赞会立刻写入空的 DIRTY_KEY，互不影响
                        Boolean hasKey = redisTemplate.hasKey(DIRTY_KEY);
                        if (Boolean.FALSE.equals(hasKey)) {
                            return;
                        }
                        // rename 可能会覆盖 key，但这里 TEMP_KEY 理论上应该是处理完删掉了的
                        // 为了安全可以使用 renameIfAbsent 或者先删后 rename，但在定时任务串行场景下直接 rename 即可
                        redisTemplate.rename(DIRTY_KEY, TEMP_KEY);

                        // 2. 取出临时 Key 里的所有 ID
                        Set<String> dirtyIds = redisTemplate.opsForSet().members(TEMP_KEY);
                        if (CollUtil.isEmpty(dirtyIds)) {
                            return;
                        }

                        // 3. 准备数据
                        Map<Long, Integer> updateMap = new HashMap<>();
                        for (String idStr : dirtyIds) {
                            Long id = Long.valueOf(idStr);
                            // 查 Redis 最新计数值
                            String countStr = redisTemplate.opsForValue().get(likedCountKeyPrefix+ id);
                            if (countStr != null) {
                                updateMap.put(id, Integer.parseInt(countStr));
                            }
                        }

                        // 4. 【核心优化】批量更新数据库 (Batch Update)
                        if (CollUtil.isNotEmpty(updateMap)) {
                            remoteBlogService.updateLikeCountBatch(updateMap);
                            log.info("同步点赞数成功，更新条数: {}", updateMap.size());
                        }

                        // 5. 处理完，删除临时 Key
                        redisTemplate.delete(TEMP_KEY);
                    }
                });

    }
}