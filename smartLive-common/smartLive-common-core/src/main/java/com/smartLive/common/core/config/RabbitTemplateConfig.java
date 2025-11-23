package com.smartLive.common.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitTemplateConfig {

    private final RabbitTemplate rabbitTemplate;
    /**
     * 监听消息发送失败返回的错误信息
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setReturnsCallback(returns -> {
            log.error("监听到了消息return callback");
            log.error("exchange:{}", returns.getExchange());
            log.error("replyCode:{}", returns.getReplyCode());
            log.error("replyText:{}", returns.getReplyText());
            log.error("message:{}", returns.getMessage());
            log.error("routingKey:{}", returns.getRoutingKey());
        }
        );
    }
}