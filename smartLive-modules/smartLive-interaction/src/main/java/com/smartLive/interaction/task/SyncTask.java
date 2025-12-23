package com.smartLive.interaction.task;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.LikeTypeEnum;
import com.smartLive.interaction.strategy.comment.CommentStrategy;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Component
@Slf4j
public class SyncTask {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 注入各个服务的 Client
    @Autowired
    private Map<Integer, LikeStrategy> likeStrategyMap;
    @Autowired
    private Map<Integer, CommentStrategy> commentStrategyMap;
    @Autowired
    private ExecutorService executorService;
    // ... 其他服务

    // 每 30 秒执行一次
    @Scheduled(cron = "0/30 * * * * ?")
    public void executeTask() throws InterruptedException {
        log.info("开始执行点赞数同步任务...");
            //执行点赞数同步
            executorService.execute(() -> {
                log.info("开始同步点赞数...");
                Arrays.stream(LikeTypeEnum.values()).forEach(type -> {
                    // 1. 只有配置了 Redis Key 的才处理
                    if (type.getLikedCountKeyPrefix() == null || type.getLikeDirtyKeyPrefix() == null) {
                        return;
                    }
                    String likedCountKeyPrefix = type.getLikedCountKeyPrefix();
                    String DIRTY_KEY = type.getLikeDirtyKeyPrefix();
                    String TEMP_KEY = DIRTY_KEY + ":TEMP";
                    // ✅ 【核心变化】直接调用策略，没有 switch-case 了！
                    LikeStrategy strategy = likeStrategyMap.get(type.getCode());
                    if (strategy != null) {
                        sync(type.getDesc(),likedCountKeyPrefix, DIRTY_KEY, TEMP_KEY, map -> strategy.transLikeCountFromRedis2DB(map));
                    } else {
                        log.warn("类型[{}]没有对应的同步策略，跳过", type.getDesc());
                    }
                });
                log.info("同步点赞数完成");
            });
            //执行评论数同步
            executorService.execute(() -> {
                log.info("开始同步评论数...");
                Arrays.stream(CommentTypeEnum.values()).forEach(commentType -> {
                    // 1. 只有配置了 Redis Key 的才处理
                    if (commentType.getCommentCountKeyPrefix() == null || commentType.getCommentDirtyKeyPrefix() == null) {
                        return;
                    }
                    String commentCountKeyPrefix = commentType.getCommentCountKeyPrefix();
                    String DIRTY_KEY = commentType.getCommentDirtyKeyPrefix();
                    String TEMP_KEY = DIRTY_KEY + ":TEMP";
                    // ✅ 【核心变化】直接调用策略，没有 switch-case 了！
                    CommentStrategy strategy = commentStrategyMap.get(commentType.getCode());

                    if (strategy != null) {
                        sync(commentType.getDesc(),commentCountKeyPrefix, DIRTY_KEY, TEMP_KEY, map -> strategy.transCommentCountFromRedis2DB(map));
                    } else {
                        log.warn("类型[{}]没有对应的同步策略，跳过", commentType.getDesc());
                    }
                });
                log.info("同步评论数完成");
            });
    }
    private void sync(String desc,String countKeyPrefix, String DIRTY_KEY, String TEMP_KEY, Consumer<Map<Long, Integer>> dbAction){
        try {
            // 2. 【原子重命名】将脏数据移入临时 Key
            if (Boolean.FALSE.equals(redisTemplate.hasKey(DIRTY_KEY))) {
                return;
            }
            // 使用 rename，如果有旧的 TEMP_KEY 没处理完，这里会覆盖（权衡之下的选择）
            // 更严谨的做法是先 check TEMP_KEY 是否存在，如果存在则报警或先合并
            redisTemplate.rename(DIRTY_KEY, TEMP_KEY);

            // 3. 取出 ID
            Set<String> dirtyIds = redisTemplate.opsForSet().members(TEMP_KEY);
            if (CollUtil.isEmpty(dirtyIds)) {
                // 即使为空，也要把临时 Key 删掉
                redisTemplate.delete(TEMP_KEY);
                return;
            }

            // 4. 组装数据
            Map<Long, Integer> updateMap = new HashMap<>();
            for (String idStr : dirtyIds) {
                Long id = Long.valueOf(idStr);
                String countStr = redisTemplate.opsForValue().get(countKeyPrefix + id);
                // 如果 countStr 为空，可能是过期了，设为 0 或者去库里查（这里视业务而定，通常设为0）
                updateMap.put(id, countStr == null ? 0 : Integer.parseInt(countStr));
            }

            // 5. 【核心修复】根据类型分发给不同的 Service
            if (CollUtil.isNotEmpty(updateMap)) {
               dbAction.accept(updateMap);
            }

            // 6. 只有同步成功了，才删除临时 Key
            redisTemplate.delete(TEMP_KEY);

        } catch (Exception e) {
            // 7. 【异常处理】
            // 如果同步失败，千万不要删 TEMP_KEY，保留现场。
            // 虽然下一次 rename 会覆盖，但至少能在日志里看到错误。
            // 进阶做法：在这里把 TEMP_KEY 的数据重新 add 回 DIRTY_KEY (回滚操作)
            log.error("同步[{}]点赞数据失败", desc, e);

            // 【可选：回滚逻辑】把数据塞回去，防止丢失
            // redisTemplate.opsForSet().add(DIRTY_KEY, dirtyIds.toArray(new String[0]));
        }
    }
}