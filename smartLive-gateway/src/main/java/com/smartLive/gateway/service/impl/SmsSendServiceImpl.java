package com.smartLive.gateway.service.impl;


import cn.hutool.core.util.RandomUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.gateway.service.SmsSendService;
import com.smartLive.gateway.until.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.ServerRequest;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SmsSendServiceImpl implements SmsSendService {

    @Resource
    StringRedisTemplate stringRedisTemplate;
    /**
     * 发送短信
     *
     * @param request
     * @return
     */
    @Override
    public Mono<ServerResponse> sendSms(ServerRequest request) {
        //获取电话号码
        String phone = request.queryParam("phone").orElse("");
        // TODO 校验手机号
        if (RegexUtils.isPhoneInvalid( phone)) {
            return ServerResponse.badRequest().bodyValue(Result.fail("手机号格式错误！"));
        }
        // TODO 发送验证码
        String code = RandomUtil.randomNumbers(4);
        // TODO 保存验证码到redis当中
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("[验证码]" + phone + ":" + code);
        return ServerResponse.ok().bodyValue(Result.ok("短信发送成功！"));
    }
}

