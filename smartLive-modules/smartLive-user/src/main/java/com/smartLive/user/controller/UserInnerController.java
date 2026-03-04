package com.smartLive.user.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.user.domain.User;
import com.smartLive.user.domain.VO.UserVO;
import com.smartLive.user.service.IUserInfoService;
import com.smartLive.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 用户服务内部Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/user")
public class UserInnerController extends BaseController
{
    @Autowired
    private IUserService userService;
    @Autowired
    private IUserInfoService userInfoService;
    /**
     * 根据手机号查询用户详情
     */
    @GetMapping("/info/{phone}")
    User getUserInfoByPhone(@PathVariable("phone") String phone){
        return userService.getUserInfoByPhone(phone);
    }
    /**
     * 创建用户
     */
    @PostMapping("/create/{phone}")
    User createUserByPhone(@PathVariable("phone") String phone){
        return userService.createUserByPhone(phone);
    }
    /**
     * 根据id查询用户列表
     */
    @GetMapping("/userListByIds")
    List<UserVO> getUserList(@RequestParam("userIdList") List<Long> userIdList){
        return userService.getUserList(userIdList);
    }
    /**
     * 根据id查询用户
     */
    @GetMapping("/getUserInfo/{id}")
    UserVO queryUserInfoById(@PathVariable("id") Long id){
        return userService.queryUserInfoById(id);
    }
    /**
     * 根据id查询用户
     */
    @GetMapping("/{id}")
    UserVO queryUserById(@PathVariable("id") Long id){
        return userService.queryUserById(id);
    }
    /**
     * 根据用户id查询用户名称
     * @param userId
     * @return
     */
    @GetMapping("/userNameById")
    String getUserNameById(@RequestParam("userId") Long userId){
        return userService.getUserNameById(userId);
    }

    /**
     * 更新用户状态
     */
    @PostMapping("/updateUserStatus")
    Boolean updateUserStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status){
        return userInfoService.updateUserStatus(id, status);
    }

    /**
     * 批量更新用户粉丝数
     */
    @PostMapping("/updateFansCountBatch")
    Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return userInfoService.updateFansCountBatch(updateMap);
    }

    /**
     * 批量更新用户关注数
     */
    @PostMapping("/updateFolloweeCountBatch")
    Boolean updateFolloweeCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return userInfoService.updateFolloweeCountBatch(updateMap);
    }

    /**
     * 批量更新用户被点赞数
     */
    @PostMapping("/updateUserLikedBatch")
    Boolean updateUserLikedBatch(@RequestBody Map<Long, Integer> updateMap){
        return userInfoService.updateUserLikedBatch(updateMap);
    }

    /**
     * 获取用户被点赞数
     */
    @GetMapping("/getUserLikedCount")
    Integer getUserLikedCount(@RequestParam("userId") Long userId){
        return userInfoService.getUserLikedCount(userId);
    }
}
