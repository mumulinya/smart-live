package com.smartLive.user.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.user.domain.UserInfo;


/**
 * 用户Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IUserInfoService extends IService<UserInfo>
{
    /**
     * 查询用户
     * 
     * @param userId 用户主键
     * @return 用户
     */
    public UserInfo selectUserInfoByUserId(Long userId);

    /**
     * 查询用户列表
     * 
     * @param userInfo 用户
     * @return 用户集合
     */
    public List<UserInfo> selectUserInfoList(UserInfo userInfo);

    /**
     * 新增用户
     * 
     * @param userInfo 用户
     * @return 结果
     */
    public int insertUserInfo(UserInfo userInfo);

    /**
     * 修改用户
     * 
     * @param userInfo 用户
     * @return 结果
     */
    public int updateUserInfo(UserInfo userInfo);

    /**
     * 批量删除用户
     * 
     * @param userIds 需要删除的用户主键集合
     * @return 结果
     */
    public int deleteUserInfoByUserIds(Long[] userIds);

    /**
     * 删除用户信息
     * 
     * @param userId 用户主键
     * @return 结果
     */
    public int deleteUserInfoByUserId(Long userId);
}
