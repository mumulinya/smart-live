package com.smartLive.gateway.filter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.domain.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.smartLive.common.core.utils.JwtUtils;
import com.smartLive.common.core.utils.ServletUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.gateway.config.properties.IgnoreWhiteProperties;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网关鉴权
 * 
 * @author smartLive
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    // 排除过滤的 uri 地址，nacos自行添加
    @Autowired
    private IgnoreWhiteProperties ignoreWhite;

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest.Builder mutate = request.mutate();

        //获取请求路径
        String url = request.getURI().getPath();
        log.info("请求地址：{}", url);
        // 提取路径前缀
        String prefix = extractPathPrefix(url);
        log.info("请求前缀：{}", prefix);
        // 后台管理系统需要校验token
        if(prefix.equals("admin")){
            // 跳过管理端不需要验证的路径
            if (StringUtils.matches(url, ignoreWhite.getAdminWhites()))
            {
                return chain.filter(exchange);
            }
            String token = getToken(request);
            if (StringUtils.isEmpty(token))
            {
                return unauthorizedResponse(exchange, "令牌不能为空");
            }
            Claims claims = JwtUtils.parseToken(token);
            if (claims == null)
            {
                return unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
            }
            String userkey = JwtUtils.getUserKey(claims);
            boolean islogin = redisService.hasKey(getTokenKey(userkey));
            if (!islogin)
            {
                return unauthorizedResponse(exchange, "登录状态已过期");
            }
            String userid = JwtUtils.getUserId(claims);
            String username = JwtUtils.getUserName(claims);
            if (StringUtils.isEmpty(userid) || StringUtils.isEmpty(username))
            {
                return unauthorizedResponse(exchange, "令牌验证失败");
            }

            // 设置用户信息到请求
            addHeader(mutate, SecurityConstants.DETAILS_FROM_SOURCE, SecurityConstants.FROM_SOURCE_ADMIN);
            addHeader(mutate, SecurityConstants.USER_KEY, userkey);
            addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userid);
            addHeader(mutate, SecurityConstants.DETAILS_USERNAME, username);
            // 内部请求来源参数清除
            removeHeader(mutate, SecurityConstants.FROM_SOURCE);
        }else{
            //app端校验token
            String token = getToken(request);
            //TODO 2.获取redis中的用户
            String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
            //如果token存在，且用户信息存在
            if(StrUtil.isNotBlank(tokenKey)&&!userMap.isEmpty()){
                //TODO 5 把hashMap对象转换为userDto对象
                UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
                userDTO.setToken(tokenKey);
                //传递用户消息到下一个服务
                // 设置用户信息到请求
                addHeader(mutate, SecurityConstants.DETAILS_FROM_SOURCE, SecurityConstants.FROM_SOURCE_APP);
                addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userDTO.getId().toString());
                addHeader(mutate, SecurityConstants.DETAILS_USERNAME, userDTO.getNickName());
                addHeader(mutate, SecurityConstants.DETAILS_USER_ICON, userDTO.getIcon());
                addHeader(mutate, SecurityConstants.DETAILS_USER_TOKEN, userDTO.getToken());
                log.info("获取当前登录用户: {}", userDTO);
            }
            // 跳过APP端不需要验证的路径
            if (StringUtils.matches(url, ignoreWhite.getAppWhites()))
            {
                return chain.filter(exchange.mutate().request(mutate.build()).build());
            }
            if(StrUtil.isBlank(token)){
                //TODO token不存在,直接放行
                return unauthorizedResponse(exchange, "未登录！");
            }
            //TODO 3.判断用户是否存在
            if(userMap.isEmpty()){
                //TODO 4 用户不存在直接放行
                return unauthorizedResponse(exchange, "登录状态已过期");
            }
            //TODO 7.刷新token有效期
            stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        }
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value)
    {
        if (value == null)
        {
            return;
        }
        String valueStr = value.toString();
        String valueEncode = ServletUtils.urlEncode(valueStr);
        mutate.header(name, valueEncode);
    }

    private void removeHeader(ServerHttpRequest.Builder mutate, String name)
    {
        mutate.headers(httpHeaders -> httpHeaders.remove(name)).build();
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg)
    {
        log.error("[鉴权异常处理]请求路径:{},错误信息:{}", exchange.getRequest().getPath(), msg);
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), msg, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 获取缓存key
     */
    private String getTokenKey(String token)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + token;
    }

    /**
     * 获取请求token
     */
    private String getToken(ServerHttpRequest request)
    {
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX))
        {
            token = token.replaceFirst(TokenConstants.PREFIX, StringUtils.EMPTY);
        }
        return token;
    }

    /**
     * 提取请求路径前缀
     * @param path
     * @return
     */
    private String extractPathPrefix(String path) {
        // 路径格式：/admin/xxx 或 /app/xxx
        if (path.startsWith("/admin/")) {
            return "admin";
        } else if (path.startsWith("/app/")) {
            return "app";
        }
        return "unknown";
    }

    @Override
    public int getOrder()
    {
        return -200;
    }
}