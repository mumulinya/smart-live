package com.smartLive.user.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import com.smartLive.user.api.factory.RemoteAppUserFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "remoteAppUserService", value = ServiceNameConstants.USER_SERVICE, fallbackFactory = RemoteAppUserFallbackFactory.class)
public interface RemoteAppUserService {

    //根据用户电话号码查询用户
    @GetMapping("/user/info/{phone}")
    R<User> getUserInfoByPhone(@PathVariable("phone") String phone);

    //电话号码创建用户
    @PostMapping("/user/create/{phone}")
    R<User> createUserByPhone(@PathVariable("phone") String phone);

    //把博客笔记推送给所有粉丝
    @PostMapping("/follow/send/blog")
    void sendBlogToFollowers(@RequestBody BlogDTO blog);
    //根据用户id字符串查询用户列表
    @GetMapping("/user/userListByIds")
    R<List<User>> getUserList(@RequestParam("userIdList") List<Long> userIdList);
    //根据用户id查询用户信息
    @GetMapping("/user/{id}")
    R<User> queryUserById(@PathVariable("id") Long id);
    //根据用户id列表查询用户列表

}
