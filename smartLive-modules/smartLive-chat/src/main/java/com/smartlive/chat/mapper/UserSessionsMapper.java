package com.smartlive.chat.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlive.chat.domain.UserSessions;

/**
 * 用户会话列表Mapper接口
 * 
 * @author 木木林
 * @date 2025-10-05
 */
public interface UserSessionsMapper extends BaseMapper<UserSessions>
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
    public List<UserSessions> selectUserSessionsList(UserSessions userSessions);

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
     * 删除用户会话列表
     * 
     * @param id 用户会话列表主键
     * @return 结果
     */
    public int deleteUserSessionsById(Long id);

    /**
     * 批量删除用户会话列表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteUserSessionsByIds(Long[] ids);
}
