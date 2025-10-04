package com.smartLive.gateway.config;

import com.smartLive.gateway.handler.SmsSendHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import com.smartLive.gateway.handler.ValidateCodeHandler;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * 路由配置信息
 * 
 * @author smartLive
 */
@Configuration
public class RouterFunctionConfiguration
{
    @Autowired
    private ValidateCodeHandler validateCodeHandler;

    @Autowired
    private SmsSendHandler smsSendHandler;

    @SuppressWarnings("rawtypes")
    /**
     * 静态资源映射
     */
    @Bean
    public RouterFunction<ServerResponse> routerFunction()
    {
        return RouterFunctions.route(
//                后台管理系统获取验证码路由
                RequestPredicates.GET("/admin/code").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                validateCodeHandler)
                .andRoute(
//                前台用户发送手机验证码路由
                RequestPredicates.POST("/app/code").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                        smsSendHandler);
    }
}
