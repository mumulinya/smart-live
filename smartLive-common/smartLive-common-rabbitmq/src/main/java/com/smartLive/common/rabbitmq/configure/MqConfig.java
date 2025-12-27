package com.smartLive.common.rabbitmq.configure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
public class MqConfig {

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter jjm = new Jackson2JsonMessageConverter();
        jjm.setCreateMessageIds(true);
        return jjm;
    }
}
