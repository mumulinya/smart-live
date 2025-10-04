package com.smartLive.auth.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.smartLive.auth.until.RegexUtils;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.LoginFormDTO;
import com.smartLive.user.api.domain.User;
import com.smartLive.user.api.domain.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录服务
 */
@Service
public class UserLoginService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RemoteAppUserService remoteAppUserService;

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    public Result login(LoginFormDTO loginForm) {
        //获取传入的电话号码和验证码
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        // TODO 获取session中的验证码
//        String sessionCode = (String) session.getAttribute("code");
        // TODO 从redis中获取验证码
        String redisCode = (String) stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+ phone);
        System.out.println("redis验证码为"+redisCode);
        //判断当前电话号码是否正确
        if (RegexUtils.isPhoneInvalid( phone)) {
            return Result.fail("手机号格式错误！");
        }
        //判断验证码和电话号码是否一致
        if (redisCode == null || !code.equals(redisCode)) {
//            return Result.fail("验证码错误");
        }
        //根据电话号码查询用户信息
        R<User> userResult = remoteAppUserService.getUserInfoByPhone(phone);
        User user = userResult.getData();
        if (user == null) {
            //不存在 创建新用户并写入数据库
            R<User> userByPhone = remoteAppUserService.createUserByPhone(phone);
            if (userByPhone.getCode() == 500) {
                return Result.fail("登录失败");
            }
            user =userByPhone.getData();
        }
        // TODO 把用户信息存入redis当中
        //将User对象转换为hashMap对象存储
        UserDTO userDto= BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDto, new HashMap<>(),
                CopyOptions.create()
                        //忽略空值
                        .setIgnoreNullValue(true)
                        //把userDto字段值转为字符串
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
        //随机生成token
        String token= UUID.randomUUID().toString();
        //存储
        String tokenKey=RedisConstants.LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        //设置token有效期
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }
    /**
     * 登出功能
     *
     * @return 无
     */
    public Result logout(HttpServletRequest request) {
        //TODO 获取请求头中的token
        String token = request.getHeader("authorization");
        //TODO 删除redis中的token
        stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY+token);
        return Result.ok();
    }
}
