package com.smartLive.follow.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.follow.domain.Follow;
import com.smartLive.follow.domain.FollowShop;
import com.smartLive.follow.mapper.FollowMapper;
import com.smartLive.follow.mapper.FollowShopMapper;

import com.smartLive.follow.service.IFollowShopService;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
public class FollowShopServiceImpl extends ServiceImpl<FollowShopMapper, FollowShop> implements IFollowShopService
{
    @Autowired
    private FollowMapper followMapper;


    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RemoteShopService remoteShopService;
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    @Override
    public Follow selectFollowById(Long id)
    {
        return followMapper.selectFollowById(id);
    }

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注
     */
    @Override
    public List<Follow> selectFollowList(Follow follow)
    {
        return followMapper.selectFollowList(follow);
    }

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int insertFollow(Follow follow)
    {
        follow.setCreateTime(DateUtils.getNowDate());
        return followMapper.insertFollow(follow);
    }

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int updateFollow(Follow follow)
    {
        return followMapper.updateFollow(follow);
    }

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键
     * @return 结果
     */
    @Override
    public int deleteFollowByIds(Long[] ids)
    {
        return followMapper.deleteFollowByIds(ids);
    }

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
    @Override
    public int deleteFollowById(Long id)
    {
        return followMapper.deleteFollowById(id);
    }


    /**
     * 关注或取关
     *
     * @param shopId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long shopId, Boolean isFollow) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_SHOP_KEY + userId;
        //判断是关注还是取关
        if(isFollow){
            //关注
            FollowShop followShop = new FollowShop();
            followShop.setUserId(userId);
            followShop.setShopId(shopId);
            followShop.setCreateTime(DateUtils.getNowDate());
            boolean save = save(followShop);
            if (save) {
                //关注成功，添加关注到redis
                stringRedisTemplate.opsForSet().add(key, shopId.toString());
            }
        }else{
            //取关
            boolean remove = remove(new QueryWrapper<FollowShop>().eq("user_id", userId).eq("shop_id", shopId));
            if (remove) {
                //取关成功，从redis中删除关注
                stringRedisTemplate.opsForSet().remove(key, shopId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 判断是否关注
     *
     * @param
     * @return
     */
    @Override
    public Result isFollowed(Long shopId) {
        //获取当前用户id
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return Result.ok(false);
        }
        Long userId = user.getId();
        String key = RedisConstants.FOLLOW_SHOP_KEY + userId;
        //判断是否关注 从redis的set集合中查询
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, shopId.toString());
//        //判断是否关注 从数据库中查询
//        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(isMember);
    }

    /**
     * 获取关注店铺列表
     *
     * @return
     */
    @Override
    public Result getFollowShops(Long userId, Integer current) {
        //获取店铺id
        List<Long> shopIdList = query()
                .select("shop_id")
                .eq("user_id", userId)
                .orderByDesc("create_time") // 添加排序
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                .getRecords()
                .stream()
                .map(FollowShop::getShopId)
                .collect(Collectors.toList());
        //根据id查询用户
       if(shopIdList.isEmpty()){
           return Result.ok(Collections.emptyList());
       }
        R<List<ShopDTO>> userSuccess = remoteShopService.getShopList(shopIdList);
        if (userSuccess.getCode() != 200) {
            return Result.fail(userSuccess.getMsg());
        }
        List<ShopDTO> shopList = userSuccess.getData();
        shopList.forEach(shopDTO -> {
            //判断是否关注
            shopDTO.setIsFollow((Boolean) isFollowed(shopDTO.getId()).getData());
        });
        return Result.ok(shopList);
    }

    /**
     * 获取关注列表
     *
     * @param userId
     * @return
     */
    @Override
    public Result getFans(Long userId) {
//        List<Long> followUserIdList = query()
//                .select("follow_user_id")
//                .eq("user_id", userId)
//                .list()
//                .stream()
//                .map(FollowShop::getUserId)  // 假设 follow 是你的实体类
//                .collect(Collectors.toList());
//                if(followUserIdList.isEmpty()){
//                    return Result.ok(Collections.emptyList());
//                }
//                R<List<User>> userSuccess = remoteAppUserService.getUserList(followUserIdList);
//                if (userSuccess.getCode() != 200) {
//                    return Result.fail(userSuccess.getMsg());
//                }
//                List<User> userList = userSuccess.getData();
//        return Result.ok(userList);
        return Result.ok(query().eq("follow_user_id", userId).list());
    }

    /**
     * 获取收藏数量
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getCollectCount(Long userId) {
        return query().eq("user_id", userId).count().intValue();
    }
}
