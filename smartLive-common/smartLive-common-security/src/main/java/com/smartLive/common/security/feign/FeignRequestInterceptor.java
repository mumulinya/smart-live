package com.smartLive.common.security.feign;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.UserDTO;
import org.springframework.stereotype.Component;
import com.smartLive.common.core.constant.SecurityConstants;
import com.smartLive.common.core.utils.ServletUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.core.utils.ip.IpUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * feign 请求拦截器
 * 
 * @author smartLive
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor
{
    @Override
    public void apply(RequestTemplate requestTemplate)
    {
        HttpServletRequest httpServletRequest = ServletUtils.getRequest();
        if (StringUtils.isNotNull(httpServletRequest))
        {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            // 传递用户信息请求头，防止丢失
            String userId = headers.get(SecurityConstants.DETAILS_USER_ID);
            if (StringUtils.isNotEmpty(userId))
            {
                requestTemplate.header(SecurityConstants.DETAILS_USER_ID, userId);
            }
            String userKey = headers.get(SecurityConstants.USER_KEY);
            if (StringUtils.isNotEmpty(userKey))
            {
                requestTemplate.header(SecurityConstants.USER_KEY, userKey);
            }
            String userName = headers.get(SecurityConstants.DETAILS_USERNAME);
            if (StringUtils.isNotEmpty(userName))
            {
                requestTemplate.header(SecurityConstants.DETAILS_USERNAME, userName);
            }
            String authentication = headers.get(SecurityConstants.AUTHORIZATION_HEADER);
            if (StringUtils.isNotEmpty(authentication))
            {
                requestTemplate.header(SecurityConstants.AUTHORIZATION_HEADER, authentication);
            }
            // 配置客户端IP
            requestTemplate.header("X-Forwarded-For", IpUtils.getIpAddr());
            requestTemplate.header(SecurityConstants.FROM_SOURCE, SecurityConstants.FROM_SOURCE_ADMIN);
        }

        UserDTO userDto = UserContextHolder.getUser();
        if (StringUtils.isNotNull(userDto))
        {
            // 传递App用户信息到请求头
            String userId = String.valueOf(userDto.getId());
            String userName = userDto.getNickName();
            String userIcon = userDto.getIcon();
            String userToken = userDto.getToken();
            if (StringUtils.isNotEmpty(userToken))
            {
                requestTemplate.header(SecurityConstants.DETAILS_USER_TOKEN, userToken);
            }
            if (StringUtils.isNotEmpty(userIcon)){
                requestTemplate.header(SecurityConstants.DETAILS_USER_ICON, userIcon);
            }
            if (StringUtils.isNotEmpty(userId))
            {
                requestTemplate.header(SecurityConstants.DETAILS_USER_ID, userId);
            }
            if (StringUtils.isNotEmpty(userName)){
                requestTemplate.header(SecurityConstants.DETAILS_USERNAME, userName);
            }
            //配置请求来源
            requestTemplate.header(SecurityConstants.FROM_SOURCE, SecurityConstants.FROM_SOURCE_APP);
        }
    }
}