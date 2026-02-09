package com.smartLive.user.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.user.api.factory.RemoteAppUserFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@FeignClient(contextId = "remoteAppUserService", value = ServiceNameConstants.USER_SERVICE, fallbackFactory = RemoteAppUserFallbackFactory.class)
public interface RemoteAppUserService {
    /**
     * 根据用户电话号码查询用户
     * @param phone
     * @return
     */
    @GetMapping("/inner/user/info/{phone}")
    UserDTO getUserInfoByPhone(@PathVariable("phone") String phone);
    /**
     * 电话号码创建用户
     * @param phone
     * @return
     */
    @PostMapping("/inner/user/create/{phone}")
    UserDTO createUserByPhone(@PathVariable("phone") String phone);

    /**
     * 根据用户id字符串查询用户列表
     * @param userIdList
     * @return
     */
    @GetMapping("/inner/user/userListByIds")
    List<UserDTO> getUserList(@RequestParam("userIdList") List<Long> userIdList);
    /**
     * 根据用户id查询用户
     * @param id
     * @return
     */
    @GetMapping("/inner/user/{id}")
    UserDTO queryUserById(@PathVariable("id") Long id);
    /**
     * 根据用户id查询用户名称
     * @param userId
     * @return
     */
    @GetMapping("/inner/user/userNameById")
    String getUserNameById(@RequestParam("userId") Long userId);
}
