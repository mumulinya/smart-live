//package com.smartlive.chat.config;
//
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.core.TopicExchange;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
////@Configuration
//public class RabbitMQConfig {
//
//    @Bean
//    public TopicExchange sessionChatTopicExchange() {
//        return new TopicExchange("session.chat.topic", true, false);
//    }
//
//    // 创建一个通用的消息处理队列
//    @Bean
//    public Queue chatMessageQueue() {
//        return new Queue("chat.message.queue", true, false, false);
//    }
//
//    // 绑定：所有 session.chat.* 的消息都路由到同一个队列
//    @Bean
//    public Binding bindingChatMessageQueue() {
//        return BindingBuilder.bind(chatMessageQueue())
//                .to(sessionChatTopicExchange())
//                .with("session.chat.*");
//    }
//}