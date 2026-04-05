package com.smartLive.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.core.enums.interaction.FollowTypeEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.interaction.LikeTypeEnum;
import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.VO.LikeVO;
import com.smartLive.interaction.mapper.LikeMapper;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.strategy.factory.LikeStrategyFactory;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞业务实现层
 * 提供全站内容的通用点赞/取消点赞功能，并实现了基于作者维度的聚合获赞统计。
 * 
 * 核心流程：
 * 1. 使用 Redis ZSet 存储资源的点赞用户列表（支持按时间排序）。
 * 2. 采用双向 ZSet 存储用户的点赞足迹。
 * 3. 实现了点赞热度的自动策略同步，确保店铺、博客等源表的点赞数最终一致。
 * 4. 自动维护“作者总获赞数”的 Redis 计数器。
 */
@Service
@Slf4j
public class likeServiceImpl extends ServiceImpl<LikeMapper, Like> implements ILikeService {
    @Autowired
    private ResourceStrategyFactory resourceStrategyFactory;
    @Autowired
    private LikeStrategyFactory likeStrategyFactory;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ZSetIdManager zSetIdManager;

    /**
     * 点赞或取消点赞核心逻辑
     * 实现原子性的 DB 记录变更与 Redis 缓存同步。
     *
     * @param like 点赞参数（sourceType, sourceId）
     * @return 操作成功返回 true
     */
    @Override
    public Boolean likeOrCancelLike(Like like) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }
        Long userId = user.getId();
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null) {
            log.error("like type invalid");
            return false;
        }

        LikeStrategy likeStrategy = likeStrategyFactory.getStrategy(like.getSourceType());
        String sourceLikeKey = sourceLikeKey(likeTypeEnum, like.getSourceId());
        String userLikeKey = userLikeKey(likeTypeEnum, userId);
        String likeCountKey = likeTypeEnum.getLikedCountKeyPrefix() + like.getSourceId();
        String likeDirtyKey = likeTypeEnum.getLikeDirtyKeyPrefix();

        boolean isLiked = redisService.getCacheZSetScore(userLikeKey, like.getSourceId().toString()) != null;
        if (!isLiked) {
            //从数据库查询
            long count = this.count(new LambdaQueryWrapper<Like>()
                    .eq(Like::getUserId, userId)
                    .eq(Like::getSourceType, like.getSourceType())
                    .eq(Like::getSourceId, like.getSourceId()));
            if (count > 0) {
                isLiked = true;
                redisService.setCacheZSet(userLikeKey, like.getSourceId().toString(), System.currentTimeMillis());
                List<Like> likeList = query()
                        .eq("source_type", like.getSourceType())
                        .eq("source_id", like.getSourceId())
                        .list();
                if (!likeList.isEmpty()) {
                    zSetIdManager.saveToZSet(sourceLikeKey, likeList, Like::getUserId, Like::getCreateTime);
                }
            }
        }

        // 获取点赞数（复用统一的三级 fallback 逻辑）
        Integer likeCount = queryLikeCount(like);

        if (isLiked) {
            boolean deleted = this.remove(new LambdaQueryWrapper<Like>()
                    .eq(Like::getUserId, userId)
                    .eq(Like::getSourceType, like.getSourceType())
                    .eq(Like::getSourceId, like.getSourceId()));
            if (deleted) {
                redisService.removeCacheZSetObject(sourceLikeKey, userId.toString());
                redisService.removeCacheZSetObject(userLikeKey, like.getSourceId().toString());
                redisService.decrementCacheValue(likeCountKey);
                redisService.setCacheSet(likeDirtyKey, like.getSourceId().toString());
                
                updateAuthorLikeCount(like, false);
            }
        } else {
            like.setUserId(userId);
            like.setCreateTime(DateUtils.getNowDate());
            boolean saved = save(like);
            if (saved) {
                redisService.setCacheZSet(sourceLikeKey, userId.toString(), System.currentTimeMillis());
                redisService.setCacheZSet(userLikeKey, like.getSourceId().toString(), System.currentTimeMillis());
                redisService.incrementCacheValue(likeCountKey);
                redisService.setCacheSet(likeDirtyKey, like.getSourceId().toString());
                likeStrategy.syncUserResource(userId, like.getSourceId());
                
                updateAuthorLikeCount(like, true);
            }
        }
        return true;
    }

    private void updateAuthorLikeCount(Like like, boolean isIncrement) {
        // 如果是评论被点赞，这里不计入用户的总点赞数
        if (GlobalBizTypeEnum.COMMENT.getCode().equals(like.getSourceType())) {
            return;
        }

        ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(like.getSourceType());
        if (resourceTypeEnum == null) return;
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(resourceTypeEnum.getCode());
        if (resourceStrategy == null) return;
        Object resource = resourceStrategy.getResourceById(like.getSourceId());
        if (resource == null) return;
        Long authorId = resourceStrategy.getAuthorId(resource);
        if (authorId == null) return;

        // 确保 Redis 中有基础值
        queryUserLikeCount(authorId);

        String userLikedCountKey = LikeTypeEnum.USER_LIKE.getLikedCountKeyPrefix() + authorId;
        String userLikedDirtyKey = LikeTypeEnum.USER_LIKE.getLikeDirtyKeyPrefix();
        if (isIncrement) {
            redisService.incrementCacheValue(userLikedCountKey);
        } else {
            redisService.decrementCacheValue(userLikedCountKey);
        }
        redisService.setCacheSet(userLikedDirtyKey, authorId.toString());
    }

    private Integer queryUserLikeCount(Long authorId) {
        String userLikedCountKey = LikeTypeEnum.USER_LIKE.getLikedCountKeyPrefix() + authorId;
        Integer likeCount = redisService.getCacheObject(userLikedCountKey);
        if (likeCount == null) {
            LikeStrategy userLikeStrategy = likeStrategyFactory.getStrategy(LikeTypeEnum.USER_LIKE.getCode());
            if (userLikeStrategy != null) {
                likeCount = userLikeStrategy.getLikeCount(authorId);
            }
            if (likeCount == null) {
                likeCount = 0;
            }
            redisService.setCacheObject(userLikedCountKey, likeCount);
        }
        return likeCount;
    }

    /**
     * 获取点赞数
     * 优先级: Redis 独立计数器 → 源数据表(策略) → like 表 COUNT
     */
    @Override
    public Integer queryLikeCount(Like like) {
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        String likeCountKey = likeTypeEnum.getLikedCountKeyPrefix() + like.getSourceId();
        // 1. 从 Redis 独立计数器读取
        Integer likeCount = redisService.getCacheObject(likeCountKey);
        if (likeCount == null) {
            // 2. 计数器不存在，从源数据表获取（如 blog.liked）
            likeCount = likeStrategyFactory.getStrategy(like.getSourceType()).getLikeCount(like.getSourceId());
            // 3. 源表也查不到，fallback 到 like 表 COUNT
            if (likeCount == null|| likeCount == 0) {
                likeCount = query().eq("source_type", like.getSourceType()).eq("source_id", like.getSourceId()).count().intValue();
            }
            // 回写 Redis 缓存
            redisService.setCacheObject(likeCountKey, likeCount);
        }
        return likeCount;
    }

    @Override
    public Integer getUserLikeCount(Like like) {
        if (like == null || like.getUserId() == null || like.getSourceType() == null) {
            return 0;
        }
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null || likeTypeEnum.getUserLikedKeyPrefix() == null) {
            return query().eq("source_type", like.getSourceType())
                    .eq("user_id", like.getUserId())
                    .count().intValue();
        }
        String userLikedKey = likeTypeEnum.getUserLikedKeyPrefix() + like.getUserId();
        if (Boolean.TRUE.equals(redisService.hasKey(userLikedKey))) {
            Long size = redisService.getCacheZSetSize(userLikedKey);
            return size == null ? 0 : size.intValue();
        }
        List<Like> likeList = query()
                .eq("source_type", like.getSourceType())
                .eq("user_id", like.getUserId())
                .orderByDesc("create_time")
                .list();
        if (likeList == null || likeList.isEmpty()) {
            return 0;
        }
        zSetIdManager.saveToZSet(userLikedKey, likeList, Like::getSourceId, Like::getCreateTime);
        return likeList.size();
    }

    @Override
    public List<LikeVO> queryLikeRecord(Like like, Integer current) {
        if (like == null) {
            return Collections.emptyList();
        }

        Long userId = like.getUserId();
        if (userId == null) {
            AppLoginUser user = UserContextHolder.getUser();
            if (user != null) {
                userId = user.getId();
                like.setUserId(userId);
            }
        }
        if (userId == null) {
            return Collections.emptyList();
        }

        int pageNo = current == null || current < 1 ? 1 : current;
        if (like.getSourceType() == null) {
            return queryAllLikeRecordOfUser(userId, pageNo);
        }

        ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(like.getSourceType());
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (resourceTypeEnum == null || likeTypeEnum == null) {
            log.error("queryLikeRecord params invalid");
            return Collections.emptyList();
        }
        return queryLikeRecordByType(userId, like.getSourceType(), pageNo, resourceTypeEnum, likeTypeEnum);
    }

    private List<LikeVO> queryAllLikeRecordOfUser(Long userId, Integer pageNo) {
        List<Like> dbList = query()
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .list();
        if (dbList == null || dbList.isEmpty()) {
            return Collections.emptyList();
        }

        int start = (pageNo - 1) * SystemConstants.MAX_PAGE_SIZE;
        if (start >= dbList.size()) {
            return Collections.emptyList();
        }
        int end = Math.min(start + SystemConstants.MAX_PAGE_SIZE, dbList.size());
        List<Like> pageList = dbList.subList(start, end);
        return buildLikeVOByLikeList(pageList);
    }

    private List<LikeVO> queryLikeRecordByType(Long userId,
                                               Integer sourceType,
                                               Integer pageNo,
                                               ResourceTypeEnum resourceTypeEnum,
                                               LikeTypeEnum likeTypeEnum) {
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(resourceTypeEnum.getCode());
        String userLikeKey = userLikeKey(likeTypeEnum, userId);
        Page<Long> sourcePage = zSetIdManager.pageIds(likeTypeEnum.getUserLikedKeyPrefix(), userId, pageNo, SystemConstants.MAX_PAGE_SIZE);
        List<Long> sourceIdList = sourcePage.getRecords();
        Map<Long, Date> likeTimeMap;

        if (sourceIdList == null || sourceIdList.isEmpty()) {
            List<Like> sourceList = query()
                    .eq("source_type", sourceType)
                    .eq("user_id", userId)
                    .orderByDesc("create_time")
                    .list();
            if (sourceList != null && !sourceList.isEmpty()) {
                zSetIdManager.saveToZSet(userLikeKey, sourceList, Like::getSourceId, Like::getCreateTime);

                int start = Math.max(0, (pageNo - 1) * SystemConstants.MAX_PAGE_SIZE);
                int end = Math.min(sourceList.size(), start + SystemConstants.MAX_PAGE_SIZE);
                if (start < end) {
                    List<Like> pageLikeList = sourceList.subList(start, end);
                    sourceIdList = pageLikeList.stream().map(Like::getSourceId).collect(Collectors.toList());
                    likeTimeMap = pageLikeList.stream().collect(Collectors.toMap(
                            Like::getSourceId,
                            Like::getCreateTime,
                            (v1, v2) -> v1,
                            LinkedHashMap::new
                    ));
                } else {
                    likeTimeMap = new HashMap<>();
                }
            } else {
                likeTimeMap = new HashMap<>();
            }
        } else {
            likeTimeMap = queryLikeTimeFromRedis(userLikeKey, sourceIdList);
        }

        if (sourceIdList == null || sourceIdList.isEmpty()) {
            return Collections.emptyList();
        }

        if (likeTimeMap.size() < sourceIdList.size()) {
            List<Like> likeList = query()
                    .eq("source_type", sourceType)
                    .eq("user_id", userId)
                    .in("source_id", sourceIdList)
                    .list();
            if (likeList != null) {
                likeList.forEach(item -> likeTimeMap.putIfAbsent(item.getSourceId(), item.getCreateTime()));
            }
        }

        List<Object> resourceList = resourceStrategy.getResourceList(sourceIdList);
        if (resourceList == null || resourceList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Object> resourceMap = new HashMap<>(resourceList.size());
        for (Object item : resourceList) {
            Long sourceId = resourceStrategy.getResourceId(item);
            if (sourceId != null) {
                resourceMap.put(sourceId, item);
            }
        }

        List<LikeVO> result = new ArrayList<>(sourceIdList.size());
        for (Long sourceId : sourceIdList) {
            Object data = resourceMap.get(sourceId);
            if (data == null) {
                continue;
            }

            Date likeTime = likeTimeMap.get(sourceId);
            if (likeTime == null) {
                Double score = redisService.getCacheZSetScore(userLikeKey, sourceId.toString());
                if (score != null) {
                    likeTime = new Date(score.longValue());
                }
            }

            result.add(new LikeVO(likeTime, data));
        }
        return result;
    }

    private List<LikeVO> buildLikeVOByLikeList(List<Like> likeList) {
        if (likeList == null || likeList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, List<Like>> grouped = likeList.stream()
                .filter(item -> item.getSourceType() != null && item.getSourceId() != null)
                .collect(Collectors.groupingBy(Like::getSourceType));
        if (grouped.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Map<Long, Object>> resourceMapByType = new HashMap<>();
        for (Map.Entry<Integer, List<Like>> entry : grouped.entrySet()) {
            Integer sourceType = entry.getKey();
            ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(sourceType);
            if (resourceTypeEnum == null) {
                continue;
            }
            ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(resourceTypeEnum.getCode());
            List<Long> sourceIds = entry.getValue().stream()
                    .map(Like::getSourceId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (sourceIds.isEmpty()) {
                continue;
            }
            List<Object> resourceList = resourceStrategy.getResourceList(sourceIds);
            if (resourceList == null || resourceList.isEmpty()) {
                continue;
            }
            Map<Long, Object> resourceMap = new HashMap<>(resourceList.size());
            for (Object item : resourceList) {
                Long sourceId = resourceStrategy.getResourceId(item);
                if (sourceId != null) {
                    resourceMap.putIfAbsent(sourceId, item);
                }
            }
            resourceMapByType.put(sourceType, resourceMap);
        }

        List<LikeVO> result = new ArrayList<>(likeList.size());
        for (Like item : likeList) {
            if (item.getSourceType() == null || item.getSourceId() == null) {
                continue;
            }
            Map<Long, Object> resourceMap = resourceMapByType.get(item.getSourceType());
            if (resourceMap == null) {
                continue;
            }
            Object data = resourceMap.get(item.getSourceId());
            if (data == null) {
                continue;
            }
            result.add(new LikeVO(item.getCreateTime(), data));
        }
        return result;
    }

    private Map<Long, Date> queryLikeTimeFromRedis(String userLikeKey, List<Long> sourceIdList) {
        if (sourceIdList == null || sourceIdList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> results = redisService.redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer keySerializer = redisService.redisTemplate.getKeySerializer();
            RedisSerializer valueSerializer = redisService.redisTemplate.getValueSerializer();
            byte[] keyBytes = keySerializer.serialize(userLikeKey);
            for (Long sourceId : sourceIdList) {
                connection.zSetCommands().zScore(keyBytes, valueSerializer.serialize(sourceId.toString()));
            }
            return null;
        });

        Map<Long, Date> likeTimeMap = new HashMap<>(sourceIdList.size());
        if (results == null || results.isEmpty()) {
            return likeTimeMap;
        }

        int limit = Math.min(sourceIdList.size(), results.size());
        for (int i = 0; i < limit; i++) {
            Object scoreObj = results.get(i);
            if (scoreObj == null) {
                continue;
            }

            long score;
            if (scoreObj instanceof Double) {
                score = ((Double) scoreObj).longValue();
            } else if (scoreObj instanceof byte[]) {
                score = Double.valueOf(new String((byte[]) scoreObj)).longValue();
            } else {
                score = Double.valueOf(Objects.toString(scoreObj)).longValue();
            }
            likeTimeMap.put(sourceIdList.get(i), new Date(score));
        }
        return likeTimeMap;
    }

    @Override
    public List<?> queryLikeUserList(Like like) {
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null) {
            log.error("like type invalid");
            return Collections.emptyList();
        }

        String sourceLikeKey = sourceLikeKey(likeTypeEnum, like.getSourceId());
        Set<Object> top5 = redisService.getCacheZSetRange(sourceLikeKey, 0, 4);
        List<Long> userIdList;

        if (top5 == null || top5.isEmpty()) {
            List<Like> userList = query()
                    .eq("source_type", like.getSourceType())
                    .eq("source_id", like.getSourceId())
                    .orderByDesc("create_time")
                    .list();
            if (userList == null || userList.isEmpty()) {
                return Collections.emptyList();
            }
            zSetIdManager.saveToZSet(sourceLikeKey, userList, Like::getUserId, Like::getCreateTime);
            userIdList = userList.stream().map(Like::getUserId).collect(Collectors.toList());
            userIdList = userIdList.size() > 4 ? userIdList.subList(0, 4) : userIdList;
        } else {
            userIdList = top5.stream().map(obj -> Long.valueOf(obj.toString())).collect(Collectors.toList());
        }

        if (userIdList.isEmpty()) {
            return Collections.emptyList();
        }

        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(FollowTypeEnum.USER_IDENTITY.getCode());
        return resourceStrategy.getResourceList(userIdList);
    }

    @Override
    public Boolean isLike(Like like) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }

        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null) {
            log.error("like type invalid");
            return false;
        }

        String userLikeKey = userLikeKey(likeTypeEnum, user.getId());
        Boolean isLike = redisService.getCacheZSetScore(userLikeKey, like.getSourceId().toString()) != null;
        if (isLike) {
            return true;
        }

        long count = this.count(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, user.getId())
                .eq(Like::getSourceType, like.getSourceType())
                .eq(Like::getSourceId, like.getSourceId()));
        if (count > 0) {
            redisService.setCacheZSet(userLikeKey, like.getSourceId().toString(), System.currentTimeMillis());
            return true;
        }
        return false;
    }
    /**
     * 批量查询用户是否点赞
     *
     * @param likeDTO
     * @param sourceIds
     * @return
     */
    @Override
    public Map<Long, Boolean> isLikeBatch(LikeDTO likeDTO, List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Long userId = null;
        AppLoginUser user = UserContextHolder.getUser();
        if (user != null) {
            userId = user.getId();
        } else if (likeDTO.getUserId() != null) {
            userId = likeDTO.getUserId();
        }

        if (userId == null) {
            return sourceIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(likeDTO.getSourceType());
        if (likeTypeEnum == null) {
            return sourceIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        String userLikeKey = userLikeKey(likeTypeEnum, userId);
        List<Object> results = redisService.redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer keySerializer = redisService.redisTemplate.getKeySerializer();
            RedisSerializer valueSerializer = redisService.redisTemplate.getValueSerializer();
            byte[] keyBytes = keySerializer.serialize(userLikeKey);
            for (Long sourceId : sourceIds) {
                connection.zSetCommands().zScore(keyBytes, valueSerializer.serialize(sourceId.toString()));
            }
            return null;
        });

        Map<Long, Boolean> resultMap = new HashMap<>(sourceIds.size());
        for (int i = 0; i < sourceIds.size(); i++) {
            resultMap.put(sourceIds.get(i), results.get(i) != null);
        }
        return resultMap;
    }
    private String sourceLikeKey(LikeTypeEnum likeTypeEnum, Long sourceId) {
        return likeTypeEnum.getLikeKeyPrefix() + sourceId;
    }

    private String userLikeKey(LikeTypeEnum likeTypeEnum, Long userId) {
        return likeTypeEnum.getUserLikedKeyPrefix() + userId;
    }
}
