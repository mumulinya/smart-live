package com.smartLive.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.enums.LikeTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.enums.StarTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.DTO.StarDTO;
import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.mapper.StarMapper;
import com.smartLive.interaction.service.IStarService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.factory.StarStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.strategy.star.StarStrategy;
import com.smartLive.interaction.tool.QueryRedisSourceIdsTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.serializer.RedisSerializer;


/**
 * 关注Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class StarServiceImpl extends ServiceImpl<StarMapper, Star> implements IStarService
{
    @Autowired
    private StarMapper starMapper;
    @Autowired
    private RedisService redisService;
    @Autowired
    private QueryRedisSourceIdsTool queryRedisSourceIdsTool;
    @Autowired
    private ResourceStrategyFactory resourceStrategyFactory;
    @Autowired
    private StarStrategyFactory starStrategyFactory;
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    @Override
    public Star selectCollectionShopById(Long id)
    {
        return starMapper.selectCollectionShopById(id);
    }

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注
     */
    @Override
    public List<Star> selectCollectionShopList(Star follow)
    {
        return starMapper.selectCollectionShopList(follow);
    }

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int insertCollectionShop(Star follow)
    {
        follow.setCreateTime(DateUtils.getNowDate());
        return starMapper.insertCollectionShop(follow);
    }

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int updateCollectionShop(Star follow)
    {
        return starMapper.updateCollectionShop(follow);
    }

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键
     * @return 结果
     */
    @Override
    public int deleteCollectionShopByIds(Long[] ids)
    {
        return starMapper.deleteCollectionShopByIds(ids);
    }

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
    @Override
    public int deleteCollectionShopById(Long id)
    {
        return starMapper.deleteCollectionShopById(id);
    }


    /**
     * 收藏或取消收藏
     *
      * @param star
     * @return
     */
    @Override
    public Boolean star(Star star) {
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }
        //获取当前用户id
        Long userId =user.getId();
        // 1. 获取对应的枚举策略
        StarTypeEnum resourceType = StarTypeEnum.getByCode(star.getSourceType());
        if (resourceType == null) {
            log.error("关注类型错误");
            return false;
        }
        StarStrategy starStrategy = starStrategyFactory.getStrategy(star.getSourceType());
        String starKeyPrefix = resourceType.getStarKeyPrefix();
        String starCountKeyPrefix = resourceType.getStarCountKeyPrefix();
        String starDirtyKeyPrefix = resourceType.getStarDirtyKeyPrefix();
        String key =starKeyPrefix+userId;
        String starCountKey =starCountKeyPrefix+star.getSourceId();
        //判断是收藏还是取消收藏
        if(star.getIsStar()){
            //收藏
            star.setUserId(userId);
            star.setCreateTime(DateUtils.getNowDate());
            boolean save = save(star);
            Integer starCount = redisService.getCacheObject(starCountKey);
            if (starCount == null) {
                starCount = starStrategy.getStarCount(star.getSourceId());
                redisService.setCacheObject(starCountKey,starCount);
            }
            if (save) {
                //收藏成功，添加收藏到redis
                redisService.setCacheZSet(key, star.getSourceId().toString(), System.currentTimeMillis());
                //记录点赞数量
                redisService.incrementCacheValue(starCountKey);
                //记录脏数据
                redisService.setCacheSet(starDirtyKeyPrefix, star.getSourceId().toString());
                //同步个人资源到es
                starStrategy.syncUserResource(userId, star.getSourceId());
            }
        }else{
            //取消收藏
            boolean remove = remove(new QueryWrapper<Star>().eq("user_id", userId).eq("source_type", star.getSourceType()).eq("source_id", star.getSourceId()));
            if (remove) {
                //取消收藏成功成功，从redis中删除收藏
                redisService.removeCacheZSetObject(key, star.getSourceId().toString());
                //记录收藏数量
                redisService.decrementCacheValue(starCountKey);
                //记录脏数据
                redisService.setCacheSet(starDirtyKeyPrefix, star.getSourceId().toString());
            }
        }
        return true;
    }

    /**
     * 判断是否收藏
     *
     * @param
     * @return
     */
    @Override
    public Boolean isStar(Star star) {
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }
        //获取当前用户id
        Long userId = user.getId();
        // 1. 获取对应的枚举策略
        StarTypeEnum resourceType = StarTypeEnum.getByCode(star.getSourceType());
        if (resourceType == null) {
            log.error("关注类型错误");
            return false;
        }
        String key =resourceType.getStarKeyPrefix()+userId;
        //判断是否关注 从redis的set集合中查询
        Boolean isMember = redisService.getCacheZSetScore(key, star.getSourceId().toString()) != null;
        if (isMember) {
            return true;
        }
       //判断是否关注 从数据库中查询
        Integer count = query().eq("user_id", userId).eq("source_type", star.getSourceType()).eq("source_id", star.getSourceId()).count().intValue();
        return count > 0;
    }

    /**
     * 获取收藏列表
     *
     * @return
     */
    @Override
    public List<?> getStarList(StarDTO starDTO, Integer current) {
        // 1. 获取对应的枚举策略
        ResourceTypeEnum resourceType = ResourceTypeEnum.getByCode(starDTO.getSourceType());
        StarTypeEnum starTypeEnum = StarTypeEnum.getByCode(starDTO.getSourceType());
        if (resourceType == null) {

            return Collections.emptyList();
        }
        //根据关注类型从关注策略工程获取bean
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(resourceType.getCode());
        //从redis获取
        Page<Long> fanIdPage = queryRedisSourceIdsTool.queryRedisIdPage(starTypeEnum.getStarKeyPrefix(), starDTO.getUserId(), current, SystemConstants.DEFAULT_PAGE_SIZE);
        List<Long> sourceIdList = fanIdPage.getRecords();
        //redis获取失败，从数据库获取
        if (sourceIdList.isEmpty()) {
            //获取粉丝id
            List<Star> sourceList = query()
                    .eq("source_type",starDTO.getSourceType())
                    .eq("user_id", starDTO.getUserId())
                    .orderByDesc("create_time") // 添加排序
                    .list();
            if(!sourceList.isEmpty()){
                sourceIdList = sourceList.stream().map(Star::getSourceId).collect(Collectors.toList());
                //截取
                if(!sourceIdList.isEmpty()){
                    //截取当前页
                    sourceIdList = sourceIdList.size() > SystemConstants.DEFAULT_PAGE_SIZE ? sourceIdList.subList((current-1)*SystemConstants.DEFAULT_PAGE_SIZE, (current-1)*SystemConstants.DEFAULT_PAGE_SIZE + SystemConstants.DEFAULT_PAGE_SIZE) : sourceIdList;
                }
                //存入redis
                saveStarIdListToRedis(starTypeEnum.getStarKeyPrefix()+starDTO.getUserId(),sourceList);
            }
        }
        //根据id查询数据
       if(sourceIdList.isEmpty()){
           return Collections.emptyList();
       }
        List<?> resourceList = resourceStrategy.getResourceList(sourceIdList);
        return resourceList;
    }

    /**
     * 获取收藏数量
     *
     * @param star
     * @return
     */
    @Override
    public Integer getStarCount(Star star) {
        StarTypeEnum starTypeEnum = StarTypeEnum.getByCode(star.getSourceType());
        String starCountKey = starTypeEnum.getStarCountKeyPrefix() + star.getSourceId();
        Integer starCount = redisService.getCacheObject(starCountKey);
        if (starCount == null) {
            //从数据库获取点赞数量并且写入到redis
            starCount = starStrategyFactory.getStrategy(star.getSourceType()).getStarCount(star.getSourceId());
            redisService.setCacheObject(starCountKey, starCount);
        }
        return starCount;
    }

    /**
     * 获取用户收藏数量
     *
     * @param star@return
     */
    @Override
    public Integer getUserStarCount(Star star) {
        Integer count = query().eq("user_id", star.getUserId()).eq("source_type", star.getSourceType()).count().intValue();
        return count;
    }

    /**
     * 保存id列表到redis
     *
     * @param key
     * @param sourceList
     */
    private void saveStarIdListToRedis(String key, List<Star> sourceList) {
        log.info("保存关注列表到Redis{}",sourceList);
        Set<ZSetOperations.TypedTuple<String>> followIdListSet = sourceList.stream()
                .map(t -> {
                    ZSetOperations.TypedTuple<String> tuple = new DefaultTypedTuple<>(t.getSourceId().toString(), (double) t.getCreateTime().getTime());
                    return tuple;
                })
                .collect(Collectors.toSet());
        redisService.setCacheZSet(key, followIdListSet);
    }

    /**
     * 批量查询是否收藏
     *
     * @param starDTO
     * @param sourceIds
     * @return
     */
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

        StarTypeEnum resourceType = StarTypeEnum.getByCode(starDTO.getSourceType());
        if (resourceType == null) {
            return sourceIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }
        
        // Key is user specific: star:{type}:user:{userId}
        String key = resourceType.getStarKeyPrefix() + userId;

        // Pipeline execution for batch check (checking multiple members in one key)
        List<Object> results = redisService.redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer keySerializer = redisService.redisTemplate.getKeySerializer();
            RedisSerializer valueSerializer = redisService.redisTemplate.getValueSerializer();
            byte[] keyBytes = keySerializer.serialize(key);

            for (Long sourceId : sourceIds) {
                // ZSCORE key member
                connection.zSetCommands().zScore(keyBytes, valueSerializer.serialize(sourceId.toString()));
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
