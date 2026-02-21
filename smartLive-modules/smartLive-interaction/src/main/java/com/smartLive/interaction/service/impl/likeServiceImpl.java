package com.smartLive.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.enums.FollowTypeEnum;
import com.smartLive.common.core.enums.LikeTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.mapper.LikeMapper;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.strategy.factory.LikeStrategyFactory;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.tool.QueryRedisSourceIdsTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import com.smartLive.interaction.api.DTO.LikeDTO;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.serializer.RedisSerializer;


/**
 * 点赞记录Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class likeServiceImpl extends ServiceImpl<LikeMapper, Like> implements ILikeService {
    @Autowired
    private ResourceStrategyFactory resourceStrategyFactory;
    @Autowired
    private LikeStrategyFactory likeStrategyFactory;
    @Autowired
    private  RedisService redisService;
    @Autowired
    private ZSetIdManager zSetIdManager;

    /**
     * 点赞或取消点赞
     *
     * @param like
     * @return 点赞记录
     */
    @Override
    public Boolean likeOrCancelLike(Like like) {
        //获取当前登录用户
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            //未登录
            return false;
        }
        Long userId = user.getId();
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null) {
            log.error("点赞类型错误");
            return false;
        }
        LikeStrategy likeStrategy = likeStrategyFactory.getStrategy(like.getSourceType());
        String likeKeyPrefix = likeTypeEnum.getLikeKeyPrefix();
        String likedCountKeyPrefix = likeTypeEnum.getLikedCountKeyPrefix();
        String likeDirtyKeyPrefix = likeTypeEnum.getLikeDirtyKeyPrefix();

        //判断当前用户是否已经点赞
        String key = likeKeyPrefix+ like.getSourceId();
        String likeCountKey = likedCountKeyPrefix + like.getSourceId();
        Double score = redisService.getCacheZSetScore(key, userId.toString());
        boolean isLiked = false;
        if (score != null) {
            // Redis 里有，肯定是点赞了
            isLiked = true;
        } else {
            // 3. 【第二层判断】Redis 里没有，必须查数据库确认！(防止缓存过期导致的误判)
            long count = this.count(new LambdaQueryWrapper<Like>()
                    .eq(Like::getUserId, userId)
                    .eq(Like::getSourceType, like.getSourceType())
                    .eq(Like::getSourceId, like.getSourceId()));
            if (count > 0) {
                isLiked = true;
                //把点赞用户列表添加到redis
                List<Like> likeList = query()
                        .eq("source_type", like.getSourceType())
                        .eq("source_id", like.getSourceId())
                        .list();
                saveLikeIdListToRedis(key, likeList);
            }
        }
        //获取点赞数量
        Integer likeCount = redisService.getCacheObject(likeCountKey);
        if (likeCount == null) {
            //从数据库获取点赞数量并且写入到redis
            likeCount = likeStrategy.getLikeCount(like.getSourceId());
            redisService.setCacheObject(likeCountKey, likeCount);
        }
        if (isLiked) {
            //删除点赞记录
            boolean isDelete = this.remove(new LambdaQueryWrapper<Like>()
                    .eq(Like::getUserId, userId)
                    .eq(Like::getSourceType, like.getSourceType())
                    .eq(Like::getSourceId, like.getSourceId()));
            if (isDelete) {
                //删除用户点赞信息
                redisService.removeCacheZSetObject(key, userId.toString());
                //记录点赞数量
                redisService.decrementCacheValue(likeCountKey);
                //记录脏数据
                redisService.setCacheSet(likeDirtyKeyPrefix, like.getSourceId().toString());
            }
        }else{
            like.setUserId(userId);
            like.setCreateTime(DateUtils.getNowDate());
            //未点赞
            boolean isSuccess = save(like);
            //保存用户点赞信息到redis的set集合 zadd key value score
            if (isSuccess) {
                //保存用户点赞信息
                redisService.setCacheZSet(key, userId.toString(), System.currentTimeMillis());
                //记录点赞数量
                redisService.incrementCacheValue(likeCountKey);
                //记录脏数据
                redisService.setCacheSet(likeDirtyKeyPrefix, like.getSourceId().toString());
                //保存用户点赞资源到es
                likeStrategy.syncUserResource(userId, like.getSourceId());
            }
        }
        return true;
    }

    /**
     * 查询点赞数
     *
     * @param like@return 点赞数
     */
    @Override
    public Integer queryLikeCount(Like like) {
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        String likeCountKey = likeTypeEnum.getLikedCountKeyPrefix() + like.getSourceId();
        Integer likeCount = redisService.getCacheObject(likeCountKey);
        if (likeCount == null) {
            //从数据库获取点赞数量并且写入到redis
            likeCount = likeStrategyFactory.getStrategy(like.getSourceType()).getLikeCount(like.getSourceId());
            redisService.setCacheObject(likeCountKey, likeCount);
        }
        return likeCount;
    }
    /**
     * 获取用户点赞数
     *
     * @param
     * @return 点赞数
     */
    @Override
    public Integer getUserLikeCount(Like like) {
        Integer count = query().eq("source_type", like.getSourceType())
                .eq("user_id", like.getUserId())
                .count().intValue();
        return count;
    }
    /**
     * 查询点赞列表
     *
     * @param like@return 点赞记录
     */
    @Override
    public List<?> queryLikeRecord(Like like, Integer current) {
        ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(like.getSourceType());
        if (resourceTypeEnum == null) {
            log.error("点赞类型错误");
            return null;
        }
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(resourceTypeEnum.getCode());
        //获取资源id
        List<Long> sourceIdList = query()
                .select("source_id")
                .eq("source_type",like.getSourceType())
                .eq("user_id", like.getUserId())
                .orderByDesc("create_time") //
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE))
                .getRecords()
                .stream()
                .map(Like::getSourceId)
                .collect(Collectors.toList());
        List<?> resourceVOList = resourceStrategy.getResourceList(sourceIdList);
        return resourceVOList;
    }

    /**
     * 查询点赞用户列表
     *
     * @param like@return 点赞用户列表
     */
    @Override
    public List<?> queryLikeUserList(Like like) {
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null) {
            log.error("点赞类型错误");
            return null;
        }
        String likeKeyPrefix = likeTypeEnum.getLikeKeyPrefix();
        String key = likeKeyPrefix + like.getSourceId();
        //查询top5的点赞数 zrange key 0 4
        Set<Object> top5 =redisService.getCacheZSetRange(key, 0, 4);
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
            //写入redis
            saveLikeIdListToRedis(likeKeyPrefix+like.getSourceId(),userList);
            userIdList = userList.stream().map(Like::getUserId).collect(Collectors.toList());
            userIdList = userIdList.size() > 4  ? userIdList.subList(0, 4) : userIdList;
        }else {
            //解析其中的用户id
            userIdList = top5.stream().map(obj -> Long.valueOf(obj.toString())).collect(Collectors.toList());
        }
        if (userIdList == null || userIdList.isEmpty()) {
            return Collections.emptyList();
        }
//        IdentityStrategy identityStrategy = identityStrategyMap.get(FollowTypeEnum.USER_IDENTITY.getCode());
//        List<?> socialInfoVOList = identityStrategy.getFollowList(userIdList);
        log.info("查询点赞用户列表: {}", userIdList);
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(FollowTypeEnum.USER_IDENTITY.getCode());
        List<UserDTO> socialInfoVOList = resourceStrategy.getResourceList(userIdList);
        return socialInfoVOList;
    }

    /**
     * 判断是否点赞
     *
     * @param like@return 是否点赞
     */
    @Override
    public Boolean isLike(Like like) {
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }
        //获取当前用户id
        Long userId = user.getId();
        // 1. 获取对应的枚举策略
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null) {
            log.info("like{}",like);
            log.error("点赞类型错误");
            return false;
        }
        String key =likeTypeEnum.getLikeKeyPrefix()+ like.getSourceId();
        //判断是否关注 从redis的zSet集合中查询
        //如果分数不为 null，说明元素存在（已关注）；如果为 null，说明不存在（未关注）
        Boolean isLike = redisService.getCacheZSetScore(key, userId.toString()) != null;
        if (isLike) {
            //已点赞
            return true;
        }
        //判断是否点赞 从数据库中查询
        Long count = this.count(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getSourceType, like.getSourceType())
                .eq(Like::getSourceId, like.getSourceId()));
        return count > 0;
    }
    /**
     * 保存点赞用户列表到Redis
     *
     * @param key
      * @param likeList
     */
    private void saveLikeIdListToRedis(String key,List<Like> likeList) {
        zSetIdManager.saveToZSet(key, likeList, Like::getUserId, Like::getCreateTime);
    }

    /**
     * 批量查询是否点赞
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
        UserDTO user = UserContextHolder.getUser();
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
        String likeKeyPrefix = likeTypeEnum.getLikeKeyPrefix();

        // Pipeline execution for batch check
        Long finalUserId = userId;
        List<Object> results = redisService.redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer keySerializer = redisService.redisTemplate.getKeySerializer();
            RedisSerializer valueSerializer = redisService.redisTemplate.getValueSerializer();
            for (Long sourceId : sourceIds) {
                String key = likeKeyPrefix + sourceId;
                // ZSCORE key member
                connection.zSetCommands().zScore(
                        keySerializer.serialize(key),
                        valueSerializer.serialize(finalUserId.toString())
                );
            }
            return null;
        });

        Map<Long, Boolean> resultMap = new HashMap<>();
        for (int i = 0; i < sourceIds.size(); i++) {
            Long sourceId = sourceIds.get(i);
            Object result = results.get(i);
            // If result is not null (Double score), it means verify true
            resultMap.put(sourceId, result != null);
        }
        return resultMap;
    }
}
