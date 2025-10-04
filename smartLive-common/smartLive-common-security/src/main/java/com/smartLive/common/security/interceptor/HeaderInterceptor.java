package com.smartLive.common.security.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import com.smartLive.common.core.constant.SecurityConstants;
import com.smartLive.common.core.context.SecurityContextHolder;
import com.smartLive.common.core.utils.ServletUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.security.auth.AuthUtil;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.system.api.model.LoginUser;

import java.util.concurrent.TimeUnit;

/**
 * 自定义请求头拦截器，将Header数据封装到线程变量中方便获取
 * 注意：此拦截器会同时验证当前用户有效期自动刷新有效期
 *
 * @author smartLive
 */
@Slf4j
public class HeaderInterceptor implements AsyncHandlerInterceptor
{
    private StringRedisTemplate stringRedisTemplate;

    public HeaderInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (!(handler instanceof HandlerMethod))
        {
            return true;
        }
        //请求来源
        String source = ServletUtils.getHeader(request, SecurityConstants.DETAILS_FROM_SOURCE);
        //管理端
        if(source.equals(SecurityConstants.FROM_SOURCE_ADMIN)){
            SecurityContextHolder.setUserId(ServletUtils.getHeader(request, SecurityConstants.DETAILS_USER_ID));
            SecurityContextHolder.setUserName(ServletUtils.getHeader(request, SecurityConstants.DETAILS_USERNAME));
            SecurityContextHolder.setUserKey(ServletUtils.getHeader(request, SecurityConstants.USER_KEY));

            String token = SecurityUtils.getToken();
            if (StringUtils.isNotEmpty(token))
            {
                LoginUser loginUser = AuthUtil.getLoginUser(token);
                if (StringUtils.isNotNull(loginUser))
                {
                    AuthUtil.verifyLoginUserExpire(loginUser);
                    SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
                }
            }
        }else {
            //app端
            String userId = ServletUtils.getHeader(request, SecurityConstants.DETAILS_USER_ID);
            String userName = ServletUtils.getHeader(request, SecurityConstants.DETAILS_USERNAME);
            String userIcon = ServletUtils.getHeader(request, SecurityConstants.DETAILS_USER_ICON);
            String userToken = ServletUtils.getHeader(request, SecurityConstants.DETAILS_USER_TOKEN);
            if (StringUtils.isNotEmpty(userId))
            {
                UserDTO userDTO = new UserDTO(Long.valueOf(userId), userName, userIcon, userToken);
                //保存用户信息到ThreadLocal
                UserContextHolder.saveUser(userDTO);
            }
            //刷新token有效期
            if (StringUtils.isNotEmpty(userToken)){
                //TODO 7.刷新token有效期
                stringRedisTemplate.expire(userToken, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception
    {
        SecurityContextHolder.remove();
        UserContextHolder.removeUser();
    }
}
