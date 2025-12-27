package com.smartLive.common.rabbitmq.utils;

import com.smartLive.common.core.constant.MqConstants;
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

    private static ScheduledExecutorService scheduledExecutorService;

    @Autowired
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * 普通交换机
     */
    public static void sendMqMessage(RabbitTemplate rabbitTemplate,
                                     String exchange,
                                     String routingKey,
                                     Object messageEvent) {
        sendMqMessage(rabbitTemplate, exchange, routingKey, messageEvent, null);
    }

    /**
     * 延迟交换机
     */
    public static void sendMqMessage(RabbitTemplate rabbitTemplate,
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
        sendWithRetry(rabbitTemplate, cd);
    }

    /**
     * 普通交换机支持死信交换机回调
     */
    public static void sendMqMessage(RabbitTemplate rabbitTemplate,
                                     String exchange,
                                     String routingKey,
                                     Object messageEvent,
                                     String deadExchange,
                                     String deadRoutingKey,
                                     Integer maxRetries
    ) {
        sendMqMessage(rabbitTemplate, exchange, routingKey, messageEvent, null, deadExchange, deadRoutingKey, maxRetries);
    }

    /**
     * 延迟交换机支持死信交换机回调
     */
    public static void sendMqMessage(RabbitTemplate rabbitTemplate,
                                     String exchange,
                                     String routingKey,
                                     Object messageEvent,
                                     Integer delayTime,
                                     String deadExchange,
                                     String deadRoutingKey,
                                     Integer maxRetries
    ) {
        RetryCorrelationData cd = new RetryCorrelationData(
                UUID.randomUUID().toString(),
                messageEvent,
                exchange,
                routingKey,
                delayTime,
                deadExchange,
                deadRoutingKey,
                maxRetries
        );
        sendWithRetry(rabbitTemplate, cd);
    }

    /**
     * 发送消息并绑定消息回调 (核心修改方法)
     */
    private static void sendWithRetry(RabbitTemplate rabbitTemplate, RetryCorrelationData cd) {
        // ⭐ [核心修改] Spring Boot 3 使用 CompletableFuture
        // 使用 whenComplete 替代原来的 addCallback
        cd.getFuture().whenComplete((confirm, throwable) -> {
            if (throwable != null) {
                // 对应原来的 onFailure
                log.error("❌ 发送异常: {}", throwable.getMessage());
                // 进行消息重发
                handleRetry(rabbitTemplate, cd);
            } else {
                // 对应原来的 onSuccess
                if (confirm.isAck()) {
                    log.info("收到ConfirmCallback ack 消息发送成功");
                } else {
                    log.error("收到ConfirmCallback ack 消息发送失败！reason：{}", confirm.getReason());
                    // 进行消息重发
                    handleRetry(rabbitTemplate, cd);
                }
            }
        });

        log.info("🚀 正在发送消息，ID: {}, delayTime: {}", cd.getId(), cd.getDelayTime());

        Integer delayTime = cd.getDelayTime();
        if (delayTime != null && delayTime > 0) {
            // 有延迟 → 延迟队列消息
            rabbitTemplate.convertAndSend(
                    cd.getExchange(),
                    cd.getRoutingKey(),
                    cd.getMessage(),
                    message -> {
                        message.getMessageProperties().setDelay(delayTime);
                        return message;
                    },
                    cd  // 带上 CorrelationData 用于 confirm 回调
            );
        } else {
            // 无延迟 → 普通队列消息
            rabbitTemplate.convertAndSend(
                    cd.getExchange(),
                    cd.getRoutingKey(),
                    cd.getMessage(),
                    cd
            );
        }
    }

    // 重试处理逻辑
    private static void handleRetry(RabbitTemplate rabbitTemplate, RetryCorrelationData cd) {
        if (cd.getRetryCount() < cd.getMaxRetries()) {
            cd.setRetryCount(cd.getRetryCount() + 1);
            log.info("scheduledExecutorService为{}", scheduledExecutorService);
            // 延迟 2 秒后执行重发
            scheduledExecutorService.schedule(() -> {
                log.info("🔄 执行第 {} 次重试发送...", cd.getRetryCount());
                sendWithRetry(rabbitTemplate, cd);
            }, 2, TimeUnit.SECONDS);

        } else {
            log.error("❌ 消息发送彻底失败，执行本地补偿和持久化。ID: {}", cd.getId());
            // TODO: 判断这个消息是否是秒杀订单消息
            if (cd.getRoutingKey().equals(MqConstants.ORDER_SECKILL_ROUTING)) {
                log.error("订单消息发送失败");
            }
        }
    }
}