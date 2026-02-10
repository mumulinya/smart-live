package com.smartLive.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.user.DTO.UserInfoDTO;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.domain.VO.UserInfoVO;

import java.util.Date;
import java.util.List;

/**
 * 用户信息Service接口
 */
public interface IUserInfoService extends IService<UserInfo> {

    /**
     * 根据用户ID查询用户信息
     */
    UserInfoVO getByUserId(Long userId);

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
    List<UserInfoVO> listByUserIds(List<Long> userIds);
    /**
     * 更新用户背景图片
     */
    Boolean updateBackgroundImage(UserInfoDTO userInfoDTO);
    /**
     * 更新用户状态
     * @param id 用户ID
     * @param status 状态
     * @return
     */
    Boolean updateUserStatus(Long id, Integer status);
}