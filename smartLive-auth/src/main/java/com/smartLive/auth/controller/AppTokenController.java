package com.smartLive.auth.controller;

import com.smartLive.auth.service.UserLoginService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.domain.LoginFormDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * token 控制
 * 
 * @author smartLive
 */
@RestController
public class AppTokenController
{

    @Resource
    private UserLoginService userLoginService;
    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/appLogin")
    public Result login(@RequestBody LoginFormDTO loginForm){
        // TODO 实现登录功能
        return userLoginService.login(loginForm);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/appLogout")
    public Result logout(HttpServletRequest request){
        // TODO 实现登出功能
        return userLoginService.logout(request);
    }
}
