package com.smartLive.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.LoginUser;
import com.smartLive.common.core.enums.FollowTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.enums.StarTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.domain.DTO.StarDTO;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.mapper.StarMapper;
import com.smartLive.interaction.service.IStarService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.factory.StarStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.strategy.star.StarStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Star service.
 */
@Service
@Slf4j
public class StarServiceImpl extends ServiceImpl<StarMapper, Star> implements IStarService {
    @Autowired
    private StarMapper starMapper;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ZSetIdManager zSetIdManager;
    @Autowired
    private ResourceStrategyFactory resourceStrategyFactory;
    @Autowired
    private StarStrategyFactory starStrategyFactory;

    @Override
    public Star selectCollectionShopById(Long id) {
        return starMapper.selectCollectionShopById(id);
    }

    @Override
    public List<Star> selectCollectionShopList(Star follow) {
        return starMapper.selectCollectionShopList(follow);
    }

    @Override
    public int insertCollectionShop(Star follow) {
        follow.setCreateTime(DateUtils.getNowDate());
        return starMapper.insertCollectionShop(follow);
    }

    @Override
    public int updateCollectionShop(Star follow) {
        return starMapper.updateCollectionShop(follow);
    }

    @Override
    public int deleteCollectionShopByIds(Long[] ids) {
        return starMapper.deleteCollectionShopByIds(ids);
    }

    @Override
    public int deleteCollectionShopById(Long id) {
        return starMapper.deleteCollectionShopById(id);
    }

    @Override
    public Boolean star(Star star) {
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }

        Long userId = user.getId();
        StarTypeEnum starType = StarTypeEnum.getByCode(star.getSourceType());
        if (starType == null) {
            log.error("star type invalid");
            return false;
        }

        StarStrategy starStrategy = starStrategyFactory.getStrategy(star.getSourceType());
        String userStarKey = starType.getStarKeyPrefix() + userId;
        String sourceStarKey = starType.getSourceStarKeyPrefix() + star.getSourceId();
        String starCountKey = starType.getStarCountKeyPrefix() + star.getSourceId();
        String starDirtyKey = starType.getStarDirtyKeyPrefix();

        if (Boolean.TRUE.equals(star.getIsStar())) {
            star.setUserId(userId);
            star.setCreateTime(DateUtils.getNowDate());
            boolean saved = save(star);

            // 获取收藏数（复用统一的三级 fallback 逻辑）
            Integer starCount = getStarCount(star);

            if (saved) {
                redisService.setCacheZSet(userStarKey, star.getSourceId().toString(), System.currentTimeMillis());
                redisService.setCacheZSet(sourceStarKey, userId.toString(), System.currentTimeMillis());
                redisService.incrementCacheValue(starCountKey);
                redisService.setCacheSet(starDirtyKey, star.getSourceId().toString());
                starStrategy.syncUserResource(userId, star.getSourceId());
            }
        } else {
            boolean removed = remove(new QueryWrapper<Star>()
                    .eq("user_id", userId)
                    .eq("source_type", star.getSourceType())
                    .eq("source_id", star.getSourceId()));
            if (removed) {
                redisService.removeCacheZSetObject(userStarKey, star.getSourceId().toString());
                redisService.removeCacheZSetObject(sourceStarKey, userId.toString());
                // 确保 Redis 计数器已初始化，再递减
                getStarCount(star);
                redisService.decrementCacheValue(starCountKey);
                redisService.setCacheSet(starDirtyKey, star.getSourceId().toString());
            }
        }

        return true;
    }

    @Override
    public Boolean isStar(Star star) {
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }

        StarTypeEnum starType = StarTypeEnum.getByCode(star.getSourceType());
        if (starType == null) {
            log.error("star type invalid");
            return false;
        }

        String userStarKey = starType.getStarKeyPrefix() + user.getId();
        Boolean isMember = redisService.getCacheZSetScore(userStarKey, star.getSourceId().toString()) != null;
        if (isMember) {
            return true;
        }

        int count = query().eq("user_id", user.getId())
                .eq("source_type", star.getSourceType())
                .eq("source_id", star.getSourceId())
                .count().intValue();
        if (count > 0) {
            redisService.setCacheZSet(userStarKey, star.getSourceId().toString(), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    @Override
    public List<?> getStarList(StarDTO starDTO, Integer current) {
        ResourceTypeEnum resourceType = ResourceTypeEnum.getByCode(starDTO.getSourceType());
        StarTypeEnum starType = StarTypeEnum.getByCode(starDTO.getSourceType());
        if (resourceType == null || starType == null) {
            return Collections.emptyList();
        }

        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(resourceType.getCode());
        Page<Long> page = zSetIdManager.pageIds(starType.getStarKeyPrefix(), starDTO.getUserId(), current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> sourceIdList = page.getRecords();

        if (sourceIdList.isEmpty()) {
            List<Star> sourceList = query()
                    .eq("source_type", starDTO.getSourceType())
                    .eq("user_id", starDTO.getUserId())
                    .orderByDesc("create_time")
                    .list();
            if (!sourceList.isEmpty()) {
                sourceIdList = sourceList.stream().map(Star::getSourceId).collect(Collectors.toList());
                int start = Math.max(0, (current - 1) * SystemConstants.MAX_PAGE_SIZE);
                if (start < sourceIdList.size()) {
                    int end = Math.min(sourceIdList.size(), start + SystemConstants.MAX_PAGE_SIZE);
                    sourceIdList = sourceIdList.subList(start, end);
                } else {
                    sourceIdList = Collections.emptyList();
                }
                zSetIdManager.saveToZSet(starType.getStarKeyPrefix() + starDTO.getUserId(), sourceList, Star::getSourceId, Star::getCreateTime);
            }
        }

        if (sourceIdList.isEmpty()) {
            return Collections.emptyList();
        }

        return resourceStrategy.getResourceList(sourceIdList);
    }

    @Override
    public List<?> queryStarUserList(Star star) {
        StarTypeEnum starType = StarTypeEnum.getByCode(star.getSourceType());
        if (starType == null || star.getSourceId() == null) {
            log.error("queryStarUserList params invalid");
            return Collections.emptyList();
        }

        String sourceStarKey = starType.getSourceStarKeyPrefix() + star.getSourceId();
        Set<Object> top5 = redisService.getCacheZSetRange(sourceStarKey, 0, 4);
        List<Long> userIdList;
        if (top5 == null || top5.isEmpty()) {
            List<Star> userList = query()
                    .eq("source_type", star.getSourceType())
                    .eq("source_id", star.getSourceId())
                    .orderByDesc("create_time")
                    .list();
            if (userList == null || userList.isEmpty()) {
                return Collections.emptyList();
            }

            zSetIdManager.saveToZSet(sourceStarKey, userList, Star::getUserId, Star::getCreateTime);
            userIdList = userList.stream().map(Star::getUserId).collect(Collectors.toList());
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

    /**
     * 获取收藏数
     * 优先级: Redis 独立计数器 → 源数据表(策略) → star 表 COUNT
     */
    @Override
    public Integer getStarCount(Star star) {
        StarTypeEnum starTypeEnum = StarTypeEnum.getByCode(star.getSourceType());
        String starCountKey = starTypeEnum.getStarCountKeyPrefix() + star.getSourceId();
        // 1. 从 Redis 独立计数器读取
        Integer starCount = redisService.getCacheObject(starCountKey);
        if (starCount == null) {
            // 2. 计数器不存在，从源数据表获取（如 blog.stared、shop.star）
            starCount = starStrategyFactory.getStrategy(star.getSourceType()).getStarCount(star.getSourceId());
            // 3. 源表也查不到，fallback 到 star 表 COUNT
            if (starCount == null) {
                starCount = query().eq("source_type", star.getSourceType()).eq("source_id", star.getSourceId()).count().intValue();
            }
            // 回写 Redis 缓存
            redisService.setCacheObject(starCountKey, starCount);
        }
        return starCount;
    }

    @Override
    public Integer getUserStarCount(Star star) {
        return query().eq("user_id", star.getUserId()).eq("source_type", star.getSourceType()).count().intValue();
    }

    @Override
    public Map<Long, Boolean> isStarBatch(StarDTO starDTO, List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Long userId = null;
        UserDTO user = UserContextHolder.getUser();
        if (user != null) {
            userId = user.getId();
        } else if (starDTO.getUserId() != null) {
            userId = starDTO.getUserId();
        }

        if (userId == null) {
            return sourceIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        StarTypeEnum starType = StarTypeEnum.getByCode(starDTO.getSourceType());
        if (starType == null) {
            return sourceIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        String key = starType.getStarKeyPrefix() + userId;
        List<Object> results = redisService.redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer keySerializer = redisService.redisTemplate.getKeySerializer();
            RedisSerializer valueSerializer = redisService.redisTemplate.getValueSerializer();
            byte[] keyBytes = keySerializer.serialize(key);
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
}
