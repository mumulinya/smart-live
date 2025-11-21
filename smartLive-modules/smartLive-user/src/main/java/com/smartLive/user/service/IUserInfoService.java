package com.smartLive.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.user.domain.UserInfo;

import java.util.Date;
import java.util.List;

/**
 * 用户信息Service接口
 */
public interface IUserInfoService extends IService<UserInfo> {

    /**
     * 根据用户ID查询用户信息
     */
    UserInfo getByUserId(Long userId);

    /**
     * 更新用户信息
     */
    boolean updateUserInfo(UserInfo userInfo);

    /**
     * 更新用户城市
     */
    boolean updateCity(Long userId, String city);

    /**
     * 更新用户个人介绍
     */
    boolean updateIntroduce(Long userId, String introduce);

    /**
     * 更新用户性别
     */
    boolean updateGender(Long userId, Integer gender);

    /**
     * 更新用户生日
     */
    boolean updateBirthday(Long userId, Date birthday);

    /**
     * 更新用户积分
     */
    boolean updateCredits(Long userId, String credits);

    /**
     * 更新用户会员等级
     */
    boolean updateLevel(Long userId, String level);
    /**
     * 根据用户ID列表查询用户信息列表
     */
    List<UserInfo> listByUserIds(List<Long> userIds);
}