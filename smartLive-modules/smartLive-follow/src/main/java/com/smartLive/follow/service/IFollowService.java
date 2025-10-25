package com.smartLive.follow.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.follow.domain.Follow;
import com.smartLive.user.api.domain.BlogDTO;


import java.util.List;


/**
 * 关注Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IFollowService extends IService<Follow>
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
     * @param followUserId
     * @param isFollow
     * @return
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 判断是否关注
     * @param followUserId
     * @return
     */
    Result isFollowed(Long followUserId);

    /**
     * 共同关注
     * @param userId
     * @return
     */
    Result common(Long userId);

    /**
     * 发送博客给关注者
     * @param blogDTO
     */
    void sendBlogToFollowers(BlogDTO blogDTO);

    /**
     * 获取粉丝列表
     * @return
     */
    Result getFans(Long followUserId,Integer current);

    /**
     * 获取关注列表
     * @return
     */
    Result getFollows(Long userId,Integer current);
    /**
     * 获取关注数
     * @return
     */
    Integer getFollowCount(Long userId);
    /**
     * 获取粉丝数
     * @return
     */
    Integer getFanCount(Long userId);
    /**
     * 获取共同关注数
     * @return
     */

    Integer getCommonFollowCount(Long userId, Long currentUserId);
}
