package com.smartLive.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.mapper.UserInfoMapper;
import com.smartLive.user.service.IUserInfoService;
import com.smartLive.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户信息Service实现类
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    private IUserService userService;

    //使用懒加载，避免循环引用
    public UserInfoServiceImpl(@Lazy IUserService userService) {
        this.userService = userService;
    }

    @Override
    public UserInfo getByUserId(Long userId) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return getOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserInfo(UserInfo userInfo) {
        Long userId = UserContextHolder.getUser().getId();
        userInfo.setUserId(userId);
        if (userInfo.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        UserInfo user = getByUserId(userId);
        //没有数据，创建数据
        if(user==null){
            return save(userInfo);
        }
        UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId);
        if (userInfo.getCity() != null){
            updateWrapper.set("city", userInfo.getCity());
        }
        if (userInfo.getIntroduce() != null){
            updateWrapper.set("introduce", userInfo.getIntroduce());
        }
        if (userInfo.getGender() != null){
            updateWrapper.set("gender", userInfo.getGender());
        }
        if (userInfo.getBirthday() != null){
            updateWrapper.set("birthday", userInfo.getBirthday());
        }
        if (userInfo.getCredits() != null){
            updateWrapper.set("credits", userInfo.getCredits());
        }
        if (userInfo.getLevel() != null){
            updateWrapper.set("level", userInfo.getLevel());
        }
        updateWrapper.set("update_time", new Date());
        boolean update = update(updateWrapper);
        if (update){
            //更新用户信息成功，更新es数据
           userService.publish(new String[]{userId.toString()});
        }
        return update;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCity(Long userId, String city) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setCity(city);
        userInfo.setUpdateTime(new Date());
        return updateById(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateIntroduce(Long userId, String introduce) {
        // 验证个人介绍长度
        if (introduce != null && introduce.length() > 128) {
            throw new RuntimeException("个人介绍不能超过128个字符");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setIntroduce(introduce);
        userInfo.setUpdateTime(new Date());
        return updateById(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGender(Long userId, Integer gender) {
        // 验证性别参数
        if (gender != null && gender != 0 && gender != 1) {
            throw new RuntimeException("性别参数错误：0-男，1-女");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setGender(gender);
        userInfo.setUpdateTime(new Date());
        return updateById(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBirthday(Long userId, Date birthday) {
        // 验证生日不能超过今天
        if (birthday != null && birthday.after(new Date())) {
            throw new RuntimeException("生日不能超过今天");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setBirthday(birthday);
        userInfo.setUpdateTime(new Date());
        return updateById(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCredits(Long userId, String credits) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setCredits(credits);
        userInfo.setUpdateTime(new Date());
        return updateById(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLevel(Long userId, String level) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setLevel(level);
        userInfo.setUpdateTime(new Date());
        return updateById(userInfo);
    }

    /**
     * 根据用户ID列表查询用户信息列表
     *
     * @param userIds
     */
    @Override
    public List<UserInfo> listByUserIds(List<Long> userIds) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        return list(queryWrapper);
    }
}