package com.smartLive.common.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitTemplateConfig implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void run(String... args) throws Exception {
        rabbitTemplate.setReturnsCallback(returns -> {
            log.error("消息发送失败:{}", returns);
            log.error("消息发送失败原因:{}", returns.getReplyText());
            log.error("消息发送失败路由:{}", returns.getRoutingKey());
            log.error("消息发送失败消息:{}", returns.getMessage());
            log.error("消息发送失败交换机:{}", returns.getExchange());
            
            if (returns.getMessage() != null) {
                log.error("消息发送失败消息id:{}", returns.getMessage().getMessageProperties().getMessageId());
                log.error("消息发送失败消息类型:{}", returns.getMessage().getMessageProperties().getContentType());
                log.error("消息发送失败消息时间:{}", returns.getMessage().getMessageProperties().getTimestamp());
                log.error("消息发送失败消息过期时间:{}", returns.getMessage().getMessageProperties().getExpiration());
            }
        });
    }
}