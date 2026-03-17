package com.smartLive.user.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.User;
import com.smartLive.user.domain.VO.UserVO;

/**
 * 用户服务接口。
 *
 * @author mumulin
 * @date 2025-09-21
 */
public interface IUserService extends IService<User>
{
    /**
     * 根据用户ID查询用户信息。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User selectUserById(Long id);

    /**
     * 查询用户列表。
     *
     * @param user 查询条件
     * @return 用户列表
     */
    List<User> selectUserList(User user);

    /**
     * 新增用户。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int insertUser(User user);

    /**
     * 修改用户信息。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int updateUser(User user);

    /**
     * 批量删除用户。
     *
     * @param ids 用户ID数组
     * @return 影响行数
     */
    int deleteUserByIds(Long[] ids);

    /**
     * 根据用户ID删除用户。
     *
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteUserById(Long id);

    /**
     * 根据手机号查询用户。
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User getUserInfoByPhone(String phone);

    /**
     * 根据手机号创建用户。
     *
     * @param phone 手机号
     * @return 新建用户
     */
    User createUserByPhone(String phone);

    /**
     * 批量查询用户信息。
     *
     * @param userIdList 用户ID列表
     * @return 用户视图列表
     */
    List<UserVO> getUserList(List<Long> userIdList);

    /**
     * 根据用户ID查询用户视图。
     *
     * @param id 用户ID
     * @return 用户视图
     */
    UserVO queryUserById(Long id);

    /**
     * 获取用户统计数据。
     *
     * @param userId 用户ID
     * @return 用户统计数据
     */
    Stats getStats(Long userId);

    /**
     * 全量发布用户数据到搜索索引。
     *
     * @return 执行结果
     */
    String allPublish();

    /**
     * 发布指定用户数据到搜索索引。
     *
     * @param ids 用户ID数组
     * @return 执行结果
     */
    String publish(String[] ids);

    /**
     * 修改用户密码。
     *
     * @param userId 用户ID
     * @param passwordDTO 密码信息
     * @return 是否修改成功
     */
    Boolean updateUserPassWord(Long userId, com.smartLive.user.DTO.PasswordDTO passwordDTO);

    /**
     * 根据用户ID获取用户昵称。
     *
     * @param userId 用户ID
     * @return 用户昵称
     */
    String getUserNameById(Long userId);

    /**
     * 根据用户ID查询用户详情。
     *
     * @param id 用户ID
     * @return 用户详情
     */
    UserVO queryUserInfoById(Long id);

    /**
     * 清理用户缓存。
     *
     * @param userId 用户ID
     */
    void clearUserCache(Long userId);

    /**
     * 根据用户ID获取用户信息。
     *
     * @param userId 用户ID
     * @return 用户视图
     */
    UserVO getUserById(Long userId);

    /**
     * 按昵称模糊搜索用户列表。
     *
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    List<UserVO> searchUsers(String keyword);
}