package com.smartLive.interaction.task;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.LikeTypeEnum;
import com.smartLive.common.core.enums.ReviewTypeEnum;
import com.smartLive.common.core.enums.StarTypeEnum;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.strategy.comment.CommentStrategy;
import com.smartLive.interaction.strategy.factory.CommentStrategyFactory;
import com.smartLive.interaction.strategy.factory.LikeStrategyFactory;
import com.smartLive.interaction.strategy.factory.ReviewStrategyFactory;
import com.smartLive.interaction.strategy.factory.StarStrategyFactory;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import com.smartLive.interaction.strategy.review.ReviewStrategy;
import com.smartLive.interaction.strategy.star.StarStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Component
@Slf4j
public class SyncTask {
    @Autowired
    private RedisService redisService;

    // 注入各个服务的 Client
    @Autowired
    private LikeStrategyFactory likeStrategyFactory;
    @Autowired
    private CommentStrategyFactory commentStrategyFactory;
    @Autowired
    private StarStrategyFactory starStrategyFactory;
    @Autowired
    private ReviewStrategyFactory reviewStrategyFactory;
    @Autowired
    private ExecutorService executorService;

    // 每 30 秒执行一次
    @Scheduled(cron = "0/30 * * * * ?")
    public void executeTask() {
        log.info("开始执行数据数量同步任务...");
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
                    LikeStrategy strategy = likeStrategyFactory.getStrategy(type.getCode());
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
                    CommentStrategy strategy = commentStrategyFactory.getStrategy(commentType.getCode());

                    if (strategy != null) {
                        sync(commentType.getDesc(),commentCountKeyPrefix, DIRTY_KEY, TEMP_KEY, map -> strategy.transCommentCountFromRedis2DB(map));
                    } else {
                        log.warn("类型[{}]没有对应的同步策略，跳过", commentType.getDesc());
                    }
                });
                log.info("同步评论数完成");
            });
           //执行收藏数同步
           executorService.execute(() -> {
            log.info("开始同步收藏数...");
            Arrays.stream(StarTypeEnum.values()).forEach(starType -> {
                // 1. 只有配置了 Redis Key 的才处理
                if (starType.getStarCountKeyPrefix() == null || starType.getStarDirtyKeyPrefix() == null) {
                    return;
                }
                String starCountKeyPrefix = starType.getStarCountKeyPrefix();
                String DIRTY_KEY = starType.getStarDirtyKeyPrefix();
                String TEMP_KEY = DIRTY_KEY + ":TEMP";
                // ✅ 【核心变化】直接调用策略，没有 switch-case 了！
                StarStrategy strategy = starStrategyFactory.getStrategy(starType.getCode());

                if (strategy != null) {
                    sync(starType.getDesc(),starCountKeyPrefix, DIRTY_KEY, TEMP_KEY, map -> strategy.transStarCountFromRedis2DB(map));
                } else {
                    log.warn("类型[{}]没有对应的同步策略，跳过", starType.getDesc());
                }
            });
            log.info("同步收藏数完成");
        });
           //执行评价数同步
          executorService.execute(() -> {
            log.info("开始同步评价数...");
            Arrays.stream(ReviewTypeEnum.values()).forEach(reviewType -> {
                // 1. 只有配置了 Redis Key 的才处理
                if (reviewType.getReviewCountKeyPrefix() == null || reviewType.getReviewDirtyKeyPrefix() == null) {
                    return;
                }
                String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix();
                String DIRTY_KEY = reviewType.getReviewDirtyKeyPrefix();
                String TEMP_KEY = DIRTY_KEY + ":TEMP";
                //调用策略
                ReviewStrategy strategy = reviewStrategyFactory.getStrategy(reviewType.getCode());
                if (strategy != null) {
                    sync(reviewType.getDesc(),reviewCountKeyPrefix, DIRTY_KEY, TEMP_KEY, map -> strategy.transReviewCountFromRedis2DB(map));
                } else {
                    log.warn("类型[{}]没有对应的同步策略，跳过", reviewType.getDesc());
                }
            });
            log.info("同步评价数完成");
        });
    }
    private void sync(String desc,String countKeyPrefix, String DIRTY_KEY, String TEMP_KEY, Consumer<Map<Long, Integer>> dbAction){
        try {
            // 2. 【原子重命名】将脏数据移入临时 Key
//            if (Boolean.FALSE.equals(redisTemplate.hasKey(DIRTY_KEY))) {
//                log.warn("[{}]没有脏数据，跳过", desc);
//                return;
//            }
            if (Boolean.FALSE.equals(redisService.hasKey(DIRTY_KEY))) {
                log.warn("[{}]没有脏数据，跳过", desc);
                return;
            }
            // 使用 rename，如果有旧的 TEMP_KEY 没处理完，这里会覆盖（权衡之下的选择）
            // 更严谨的做法是先 check TEMP_KEY 是否存在，如果存在则报警或先合并
            redisService.rename(DIRTY_KEY, TEMP_KEY);
            // 3. 取出 ID
//            Set<String> dirtyIds = redisTemplate.opsForSet().members(TEMP_KEY);
            Set<Object> dirtyIds = redisService.getCacheSet(TEMP_KEY);
            log.info("[{}] dirtyIds: {}", desc, dirtyIds);
            if (CollUtil.isEmpty(dirtyIds)) {
                // 即使为空，也要把临时 Key 删掉
//                redisTemplate.delete(TEMP_KEY);
                redisService.deleteObject(TEMP_KEY);
                return;
            }

            // 4. 组装数据
            Map<Long, Integer> updateMap = new HashMap<>();
            for (Object idStr : dirtyIds) {
                Long id = Long.valueOf(idStr.toString());
//                String countStr = redisTemplate.opsForValue().get(countKeyPrefix + id);
                Object countStr = redisService.getCacheObject(countKeyPrefix + id);
                // 如果 countStr 为空，可能是过期了，设为 0 或者去库里查（这里视业务而定，通常设为0）
                updateMap.put(id, countStr == null ? 0 : Integer.parseInt(countStr.toString()));
            }

            // 5. 【核心修复】根据类型分发给不同的 Service
            if (CollUtil.isNotEmpty(updateMap)) {
               dbAction.accept(updateMap);
            }

            // 6. 只有同步成功了，才删除临时 Key
//            redisTemplate.delete(TEMP_KEY);
            redisService.deleteObject(TEMP_KEY);
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
