package com.smartLive.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.enums.IdentityTypeEnum;
import com.smartLive.common.core.enums.LikeTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import com.smartLive.interaction.mapper.LikeMapper;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.strategy.identity.IdentityStrategy;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private Map<Integer, IdentityStrategy> identityStrategyMap;
    @Autowired
    private Map<Integer, ResourceStrategy> resourceStrategyMap;
    @Autowired
    private  RedisService redisService;

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
        String likeKeyPrefix = likeTypeEnum.getLikeKeyPrefix();
        String likedCountKeyPrefix = likeTypeEnum.getLikedCountKeyPrefix();
        String likeDirtyKeyPrefix = likeTypeEnum.getLikeDirtyKeyPrefix();

        //判断当前用户是否已经点赞
        String key = likeKeyPrefix+ like.getSourceId();
        Double score = redisService.getCacheZSetScore(key, userId.toString());
        boolean isLiked = false;
        if (score != null) {
            // Redis 里有，肯定是点赞了
            isLiked = true;
        } else {
            // 3. 【第二层判断】Redis 里没有，必须查数据库确认！(防止缓存过期导致的误判)
            // 假设你使用的是 MyBatis-Plus
            long count = this.count(new LambdaQueryWrapper<Like>()
                    .eq(Like::getUserId, userId)
                    .eq(Like::getSourceType, like.getSourceType())
                    .eq(Like::getSourceId, like.getSourceId()));
            if (count > 0) {
                isLiked = true;
                // 💡 可选优化：既然数据库有但Redis没有，说明缓存丢了。
                // 此时可以顺手把 Redis 补回去 (缓存预热)，或者直接往下走取消逻辑也没问题。

            }
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
                redisService.decrementCacheValue(likedCountKeyPrefix+ like.getSourceId());
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
                redisService.incrementCacheValue(likedCountKeyPrefix+ like.getSourceId());
                //记录脏数据
                redisService.setCacheSet(likeDirtyKeyPrefix, like.getSourceId().toString());
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
        Integer likeCount = query().eq("source_type", like.getSourceType()).eq("user_id", like.getUserId()).count().intValue();
        return likeCount;
    }

    /**
     * 查询点赞列表
     *
     * @param like@return 点赞记录
     */
    @Override
    public List<ResourceVO> queryLikeRecord(Like like, Integer current) {
        ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.getByCode(like.getSourceType());
        if (resourceTypeEnum == null) {
            log.error("点赞类型错误");
            return null;
        }
        ResourceStrategy resourceStrategy = resourceStrategyMap.get(resourceTypeEnum.getCode());
        List<Long> sourceIdList = query().select("source_id").eq("source_type", like.getSourceType()).eq("user_id", like.getUserId()).list().stream().map(Like::getSourceId).collect(Collectors.toList());
        log.info("资源id：{}", sourceIdList);
        if (sourceIdList == null || sourceIdList.isEmpty()) {
            return null;
        }
        List<ResourceVO> resourceVOList = resourceStrategy.getResourceList(sourceIdList);
        return resourceVOList;
    }

    /**
     * 查询点赞用户列表
     *
     * @param like@return 点赞用户列表
     */
    @Override
    public List<SocialInfoVO> queryLikeUserList(Like like) {
        LikeTypeEnum likeTypeEnum = LikeTypeEnum.getByCode(like.getSourceType());
        if (likeTypeEnum == null) {
            log.error("点赞类型错误");
            return null;
        }
        String likeKeyPrefix = likeTypeEnum.getLikeKeyPrefix();
        String key = likeKeyPrefix + like.getSourceId();
        //查询top5的点赞数 zrange key 0 4
        Set<Object> top5 =redisService.getCacheZSetRange(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return null;
        }
        IdentityStrategy identityStrategy = identityStrategyMap.get(IdentityTypeEnum.USER_IDENTITY.getCode());
        //解析其中的用户id
        List<Long> userIdList = top5.stream().map(obj -> Long.valueOf(obj.toString())).collect(Collectors.toList());
        log.info("查询点赞用户列表: {}", userIdList);
        List<SocialInfoVO> socialInfoVOList = identityStrategy.getFollowList(userIdList);
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
            log.error("点赞类型错误");
            return false;
        }
        String key =likeTypeEnum.getLikeKeyPrefix()+ like.getSourceId();
//        //判断是否关注 从数据库中查询
//        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        //判断是否关注 从redis的zSet集合中查询
        //如果分数不为 null，说明元素存在（已关注）；如果为 null，说明不存在（未关注）
        Boolean isLike = redisService.getCacheZSetScore(key, userId.toString()) != null;
        return isLike;
    }
}
