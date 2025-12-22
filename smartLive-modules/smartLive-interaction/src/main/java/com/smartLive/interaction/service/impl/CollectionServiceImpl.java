package com.smartLive.interaction.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Collection;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.interaction.mapper.CollectionMapper;
import com.smartLive.interaction.service.ICollectionService;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.tool.QueryRedisSourceIdsTool;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关注Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
public class CollectionServiceImpl extends ServiceImpl<CollectionMapper, Collection> implements ICollectionService
{
    @Autowired
    private CollectionMapper collectionMapper;


    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private QueryRedisSourceIdsTool queryRedisSourceIdsTool;
    @Autowired
    private Map<String, ResourceStrategy> resourceStrategyMap;
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    @Override
    public Collection selectCollectionShopById(Long id)
    {
        return collectionMapper.selectCollectionShopById(id);
    }

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注
     */
    @Override
    public List<Collection> selectCollectionShopList(Collection follow)
    {
        return collectionMapper.selectCollectionShopList(follow);
    }

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int insertCollectionShop(Collection follow)
    {
        follow.setCreateTime(DateUtils.getNowDate());
        return collectionMapper.insertCollectionShop(follow);
    }

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int updateCollectionShop(Collection follow)
    {
        return collectionMapper.updateCollectionShop(follow);
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
        return collectionMapper.deleteCollectionShopByIds(ids);
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
        return collectionMapper.deleteCollectionShopById(id);
    }


    /**
     * 收藏或取消收藏
     *
      * @param collection
     * @return
     */
    @Override
    public Result collection(Collection collection) {
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return Result.ok(false);
        }
        //获取当前用户id
        Long userId = user.getId();
        // 1. 获取对应的枚举策略
        ResourceTypeEnum resourceType = ResourceTypeEnum.getByCode(collection.getSourceType());
        if (resourceType == null) {
            return Result.fail("关注类型错误");
        }
        String key =resourceType.getCollectKeyPrefix()+userId;
        //判断是关注还是取关
        if(collection.getIsCollection()){
            //关注
            collection.setCreateTime(DateUtils.getNowDate());
            boolean save = save(collection);
            if (save) {
                //关注成功，添加关注到redis
                stringRedisTemplate.opsForZSet().add(key, collection.getSourceId().toString(), System.currentTimeMillis());
            }
        }else{
            //取关
            boolean remove = remove(new QueryWrapper<Collection>().eq("user_id", userId).eq("source_type",collection.getSourceType()).eq("source_id", collection.getSourceId()));
            if (remove) {
                //取关成功，从redis中删除关注
                stringRedisTemplate.opsForZSet().remove(key, collection.getSourceId().toString());
            }
        }
        return Result.ok();
    }

    /**
     * 判断是否收藏
     *
     * @param
     * @return
     */
    @Override
    public Result isCollection(Collection collection) {
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return Result.ok(false);
        }
        //获取当前用户id
        Long userId = user.getId();
        // 1. 获取对应的枚举策略
        ResourceTypeEnum resourceType = ResourceTypeEnum.getByCode(collection.getSourceType());
        if (resourceType == null) {
            return Result.fail("关注类型错误");
        }
        String key =resourceType.getCollectKeyPrefix()+userId;
        //判断是否关注 从redis的set集合中查询
        Boolean isMember = stringRedisTemplate.opsForZSet().score(key, collection.getSourceId().toString()) != null;
//        //判断是否关注 从数据库中查询
//        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(isMember);
    }

    /**
     * 获取收藏列表
     *
     * @return
     */
    @Override
    public Result getCollectionList(Collection collection, Integer current) {
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return Result.ok(false);
        }
        //获取当前用户id
        Long userId = user.getId();
        // 1. 获取对应的枚举策略
        ResourceTypeEnum resourceType = ResourceTypeEnum.getByCode(collection.getSourceType());
        if (resourceType == null) {
            return Result.fail("关注类型错误");
        }
        //根据关注类型从关注策略工程获取bean
        ResourceStrategy resourceStrategy = resourceStrategyMap.get(resourceType.getBizDomain()+"ResourceStrategy");
        //从redis获取
        Page<Long> fanIdPage = queryRedisSourceIdsTool.queryRedisIdPage(resourceType.getCollectKeyPrefix(), userId, current, SystemConstants.DEFAULT_PAGE_SIZE);
        List<Long> sourceIdList = fanIdPage.getRecords();
        if (sourceIdList.isEmpty()) {
            //redis获取失败，从数据库获取
            //获取来源id
             sourceIdList = query()
                    .select("shop_id")
                    .eq("source_type", collection.getSourceType())
                    .eq("user_id", collection.getUserId())
                    .orderByDesc("create_time") // 添加排序
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                    .getRecords()
                    .stream()
                    .map(Collection::getSourceId)
                    .collect(Collectors.toList());
        }
        if (sourceIdList.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //根据id查询用户
       if(sourceIdList.isEmpty()){
           return Result.ok(Collections.emptyList());
       }
        List<ResourceVO> resourceList = resourceStrategy.getResourceList(sourceIdList);
        return Result.ok(resourceList);
    }
    /**
     * 获取收藏数量
     *
     * @param collection
     * @return
     */
    @Override
    public Integer getCollectCount(Collection collection) {
        return query().eq("source_type", collection.getSourceType()).eq("user_id", collection.getUserId()).count().intValue();
    }
}
