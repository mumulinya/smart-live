package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.api.dto.FeedEventDTO;
import com.smartLive.interaction.domain.Follow;
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
    public Follow selectFollowUserById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
    public List<Follow> selectFollowUserList(Follow follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int insertFollowUser(Follow follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int updateFollowUser(Follow follow);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键集合
     * @return 结果
     */
    public int deleteFollowUserByIds(Long[] ids);

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
    public int deleteFollowUserById(Long id);

    /**
     * 关注或取关
     * @param
     * @return
     */
    Result follow(Follow follow);

    /**
     * 判断是否关注
     * @param follow
     * @return
     */
    Result isFollowed(Follow follow);

    /**
     * 共同关注列表
      * @param follow
     * @return
     */
    Result common(Follow follow, Integer current);

    /**
     * 发送博客给关注者
     * @param blogDTO
     */
    void sendBlogToFollowers(BlogDTO blogDTO);

    /**
     * 推送数据给粉丝
     */
    void pushToFollowers(FeedEventDTO feedEventDTO);

    /**
     * 获取粉丝列表
     * @return
     */
    Result getFans(Follow follow,Integer current);

    /**
     * 获取关注列表
     * @return
     */
    Result getFollows(Follow follow,Integer current);
    /**
     * 获取关注数
     * @return
     */
    Integer getFollowCount(Follow follow);
    /**
     * 获取粉丝数
     * @return
     */
    Integer getFanCount(Follow follow);
    /**
     * 获取共同关注数
     * @return
     */

    Integer getCommonFollowCount(Follow follow);
}
