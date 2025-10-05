package com.smartlive.chat.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlive.chat.domain.UserSessions;

/**
 * 用户会话列表Service接口
 * 
 * @author 木木林
 * @date 2025-10-05
 */
public interface IUserSessionsService extends IService<UserSessions>
{
    /**
     * 查询用户会话列表
     * 
     * @param id 用户会话列表主键
     * @return 用户会话列表
     */
    public UserSessions selectUserSessionsById(Long id);

    /**
     * 查询用户会话列表列表
     * 
     * @param userSessions 用户会话列表
     * @return 用户会话列表集合
     */
    public List<UserSessions> selectUserSessionsList(UserSessions userSessions,Integer current);

    /**
     * 新增用户会话列表
     * 
     * @param userSessions 用户会话列表
     * @return 结果
     */
    public int insertUserSessions(UserSessions userSessions);

    /**
     * 修改用户会话列表
     * 
     * @param userSessions 用户会话列表
     * @return 结果
     */
    public int updateUserSessions(UserSessions userSessions);

    /**
     * 批量删除用户会话列表
     * 
     * @param ids 需要删除的用户会话列表主键集合
     * @return 结果
     */
    public int deleteUserSessionsByIds(Long[] ids);

    /**
     * 删除用户会话列表信息
     * 
     * @param id 用户会话列表主键
     * @return 结果
     */
    public int deleteUserSessionsById(Long id);
}
