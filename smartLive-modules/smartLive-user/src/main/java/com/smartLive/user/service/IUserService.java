package com.smartLive.user.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.User;

/**
 * 用户Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IUserService extends IService<User>
{
    /**
     * 查询用户
     * 
     * @param id 用户主键
     * @return 用户
     */
    public User selectUserById(Long id);

    /**
     * 查询用户列表
     * 
     * @param user 用户
     * @return 用户集合
     */
    public List<User> selectUserList(User user);

    /**
     * 新增用户
     * 
     * @param user 用户
     * @return 结果
     */
    public int insertUser(User user);

    /**
     * 修改用户
     * 
     * @param user 用户
     * @return 结果
     */
    public int updateUser(User user);

    /**
     * 批量删除用户
     * 
     * @param ids 需要删除的用户主键集合
     * @return 结果
     */
    public int deleteUserByIds(Long[] ids);

    /**
     * 删除用户信息
     * 
     * @param id 用户主键
     * @return 结果
     */
    public int deleteUserById(Long id);

    /**
     * 根据用户电话号码查询用户
     * @param phone 手机号
     * @return 用户
     */
    R<User> getUserInfoByPhone(String phone);

    /**
     * 电话号码创建用户
     * @param phone 手机号
     * @return 用户
     */
    R<User> createUserByPhone(String phone);

    /**
     * 根据用户id列表查询用户列表
     * @param userIdList 用户id列表
     * @return 用户列表
     */
    R<List<User>> getUserList(List<Long> userIdList);
    /**
     * 根据用户id查询用户
     * @param id 用户id
     * @return 用户
     */
    User queryUserById(Long id);

    /**
     * 获取用户统计信息
     * @param userId 用户id
     * @return 用户统计信息
     */
    Stats getStats(Long userId);

    /**
     * 全部发布
     *
     * @return 全部发布结果
     */
    String allPublish();

    /**
     * 发布
     *
     * @param
     * @return 发布结果
     */
    String publish( String[] ids);}
