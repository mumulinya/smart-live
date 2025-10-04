package com.smartLive.gateway.service;

import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.ServerRequest;

public interface SmsSendService {
      /**
       * 发送短信
       * @param request
       * @return
       */
      public Mono<ServerResponse> sendSms(ServerRequest request);
}
