package com.smartLive.common.rabbitmq.utils;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import com.smartLive.common.rabbitmq.domain.RetryCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mq消息发送工具类
 * Spring Boot 3.x / JDK 17 适配版
 */
@Slf4j
@Component
public class MqMessageSendUtils {
    private final ScheduledExecutorService scheduledExecutorService;
    private final RabbitTemplate rabbitTemplate;
    @Autowired
    public MqMessageSendUtils(ScheduledExecutorService scheduledExecutorService, RabbitTemplate rabbitTemplate) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 普通交换机
     */
    public void sendMqMessage(
            String exchange,
            String routingKey,
            Object messageEvent)
    {
        RetryCorrelationData cd = new RetryCorrelationData(
                UUID.randomUUID().toString(),
                messageEvent,
                exchange,
                routingKey,
                3
        );
        sendWithRetry(cd);
    }

    /**
     * 延迟交换机
     */
    public void sendMqMessage(
            String exchange,
            String routingKey,
            Object messageEvent,
            Integer delayTime) {
        RetryCorrelationData cd = new RetryCorrelationData(
                UUID.randomUUID().toString(),
                messageEvent,
                exchange,
                routingKey,
                delayTime,
                3
        );
        sendWithRetry(cd);
    }

    /**
     * 发送消息并绑定消息回调 (核心修改方法)
     */
    private void sendWithRetry(RetryCorrelationData cd) {
        /**
         * 绑定消息回调
         * 1. 发送消息成功，消息acked
         * 2. 发送消息失败，消息nacked
         */
        cd.getFuture().whenComplete((confirm, throwable) -> {
            if (throwable != null) {
                // 对应原来的 onFailure
                log.error("❌ 发送异常: {}", throwable.getMessage());
                // 进行消息重发
                handleRetry(cd);
            } else {
                // 对应原来的 onSuccess
                if (confirm.isAck()) {
                    log.info("收到ConfirmCallback ack 消息发送成功");
                } else {
                    log.error("收到ConfirmCallback ack 消息发送失败！reason：{}", confirm.getReason());
                    // 进行消息重发
                    handleRetry(cd);
                }
            }
        });

        log.info("🚀 正在发送消息，ID: {}, delayTime: {}", cd.getId(), cd.getDelayTime());

        Integer delayTime = cd.getDelayTime();
        if (delayTime != null && delayTime > 0) {
            // 有延迟 → 延迟队列消息
            this.rabbitTemplate.convertAndSend(
                    cd.getExchange(),
                    cd.getRoutingKey(),
                    cd.getMessage(),
                    message -> {
                        message.getMessageProperties().setDelay(delayTime);
                        message.getMessageProperties().setMessageId(cd.getId());
                        return message;
                    },
                    cd  // 带上 CorrelationData 用于 confirm 回调
            );
        } else {
            // 无延迟 → 普通队列消息
            this.rabbitTemplate.convertAndSend(
                    cd.getExchange(),
                    cd.getRoutingKey(),
                    cd.getMessage(),
                    message -> {
                        message.getMessageProperties().setMessageId(cd.getId());
                        return message;
                    },
                    cd
            );
        }
    }

    // 重试处理逻辑
    private  void handleRetry(RetryCorrelationData cd) {
        if (cd.getRetryCount() < cd.getMaxRetries()) {
            cd.setRetryCount(cd.getRetryCount() + 1);
            log.info("scheduledExecutorService为{}", this.scheduledExecutorService);
            // 延迟 2 秒后执行重发
            scheduledExecutorService.schedule(() -> {
                log.info("🔄 执行第 {} 次重试发送...", cd.getRetryCount());
                sendWithRetry(cd);
            }, 2, TimeUnit.SECONDS);

        } else {
            log.error("❌ 消息发送彻底失败，执行本地补偿和持久化。ID: {}", cd.getId());
            // TODO: 判断这个消息是否是秒杀订单消息
            if (cd.getRoutingKey().equals(OrderMqConstants.ORDER_SECKILL_ROUTING_KEY)) {
                log.error("订单消息发送失败");
            }
        }
    }
}