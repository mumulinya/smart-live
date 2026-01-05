package com.smartLive.user.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.user.domain.User;
import com.smartLive.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    List<User> getUserList(@RequestParam("userIdList") List<Long> userIdList){
        return userService.getUserList(userIdList);
    }
    /**
     * 根据id查询用户
     */
    @GetMapping("/{id}")
    User queryUserById(@PathVariable("id") Long id){
        return userService.queryUserById(id);
    }
}
