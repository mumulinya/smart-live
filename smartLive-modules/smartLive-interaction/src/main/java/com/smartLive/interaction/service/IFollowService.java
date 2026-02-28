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
     *
     * @param follow 关注实体
     * @return 操作结果
     */
    Boolean follow(Follow follow);

    /**
     * 判断是否已关注
     *
     * @param follow 关注查询条件
     * @return 是否已关注
     */
    Boolean isFollowed(Follow follow);

    /**
     * 获取共同关注列表
     *
     * @param follow  关注查询条件
     * @param current 当前页码
     * @return 共同关注列表
     */
    List<?> common(Follow follow, Integer current);
    /**
     * 推送Feed动态给粉丝
     *
     * @param feedEventMessage Feed事件消息
     */
    void pushToFollowers(FeedEventMessage feedEventMessage);

    /**
     * 获取粉丝列表（分页）
     *
     * @param follow  关注查询条件
     * @param current 当前页码
     * @return 粉丝列表
     */
    List<?> getFans(Follow follow, Integer current);

    /**
     * 获取关注列表（分页）
     *
     * @param followDTO 关注查询条件
     * @param current   当前页码
     * @return 关注列表
     */
    List<?> getFollows(FollowDTO followDTO, Integer current);
    /**
     * 获取关注数
     *
     * @param follow 关注查询条件
     * @return 关注数量
     */
    Integer getFollowCount(Follow follow);
    /**
     * 获取粉丝数
     *
     * @param follow 关注查询条件
     * @return 粉丝数量
     */
    Integer getFanCount(Follow follow);
    /**
     * 获取共同关注数
     *
     * @param follow 关注查询条件
     * @return 共同关注数量
     */
    Integer getCommonFollowCount(Follow follow);
}
