package com.smartLive.user.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.DTO.UserInfoDTO;
import com.smartLive.user.domain.User;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.service.IUserInfoService;
import com.smartLive.user.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 用户信息Controller
 */
@RestController
@RequestMapping("/user/info")
public class UserInfoController {

    @Autowired
    private IUserInfoService userInfoService;
    @Autowired
    private IUserService userService;
    /**
     * 获取用户信息
     */
    @GetMapping("/getUserInfo/{userId}")
    public Result getUserInfo( @PathVariable("userId") Long userId) {
        try {
            UserInfo userInfo = userInfoService.getByUserId(userId);
            if (userInfo == null) {
                return Result.ok("");
            }
            User byId = userService.getById(userId);
            if (byId.getPassword() == null || " ".equals(byId.getPassword())) {
                userInfo.setHasPassword(false);
            }else {
                userInfo.setHasPassword(true);
            }
            return Result.ok(userInfo);
        } catch (Exception e) {
            return Result.fail("获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public Result updateUserInfo(@RequestBody UserInfoDTO userInfoDTO) {
        try {
            System.out.println("userInfoDTO: " + userInfoDTO);
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(userInfoDTO, userInfo);
            userInfo.setUpdateTime(new Date());
            
            boolean success = userInfoService.updateUserInfo(userInfo);
            return success ? Result.ok(true) : Result.fail("更新用户信息失败");
            
        } catch (Exception e) {
            return Result.fail("更新用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新背景图
     */
    @PostMapping("/updateBackgroundImage")
    public Result updateBackgroundImage(@RequestBody UserInfoDTO userInfoDTO) {
        Boolean result = userInfoService.updateBackgroundImage(userInfoDTO);
        if (result) {
            return Result.ok("操作成功");
        } else {
            return Result.fail("操作失败");
        }
    }
}