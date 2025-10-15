package com.smartLive.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.domain.Follow;
import com.smartLive.user.domain.FollowShop;

import java.util.List;


/**
 * 关注Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IFollowShopService extends IService<FollowShop>
{
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    public Follow selectFollowById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
    public List<Follow> selectFollowList(Follow follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int insertFollow(Follow follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int updateFollow(Follow follow);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键集合
     * @return 结果
     */
    public int deleteFollowByIds(Long[] ids);

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
    public int deleteFollowById(Long id);

    /**
     * 关注或取关
     * @param shopId
     * @param isFollow
     * @return
     */
    Result follow(Long shopId, Boolean isFollow);

    /**
     * 判断是否关注
     * @param followUserId
     * @return
     */
    Result isFollowed(Long followUserId);

    /**
     * 获取粉丝列表
     * @return
     */
    Result getFans(Long followUserId);

    /**
     * 获取店铺列表
     * @return
     */
    Result getFollowShops(Long  userId,Integer current);

    /**
     * 获取收藏数量
     * @param userId
     * @return
     */
    Integer getCollectCount(Long userId);
}
