package com.smartLive.gateway.handler;

import com.smartLive.gateway.service.SmsSendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * 短信发送处理
 *
 * @author ruoyi
 */
@Component
public class SmsSendHandler implements HandlerFunction<ServerResponse> {
    @Autowired
    private SmsSendService smsSendService;
    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        return smsSendService.sendSms(request);
    }
}
