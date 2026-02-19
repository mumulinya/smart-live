package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.interaction.domain.DTO.FollowDTO;
import com.smartLive.interaction.domain.Follow;
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
     Follow selectFollowUserById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
     List<Follow> selectFollowUserList(Follow follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int insertFollowUser(Follow follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
     int updateFollowUser(Follow follow);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键集合
     * @return 结果
     */
     int deleteFollowUserByIds(Long[] ids);

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
     int deleteFollowUserById(Long id);

    /**
     * 关注或取关
     * @param
     * @return
     */
    Boolean follow(Follow follow);

    /**
     * 判断是否关注
     * @param follow
     * @return
     */
    Boolean isFollowed(Follow follow);

    /**
     * 共同关注列表
      * @param follow
     * @return
     */
    List<?> common(Follow follow, Integer current);
    /**
     * 推送数据给粉丝
     */
    void pushToFollowers(FeedEventMessage feedEventMessage);

    /**
     * 获取粉丝列表
     * @return
     */
    List<?> getFans(Follow follow,Integer current);

    /**
     * 获取关注列表
     * @return
     */
    List<?> getFollows(FollowDTO followDTO, Integer current);
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
