package com.smartLive.common.core.utils.rabbitMq;

import com.smartLive.common.core.domain.RetryCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 死信队列发送工具类
 */
@Slf4j
public class MqDeadLetterSendUtils {

    // 定义一个全局的调度线程池
    private static final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(5);

    /**
     * 普通交换机
     */
    public static void sendMqMessage(RabbitTemplate rabbitTemplate,
                                     String exchange,
                                     String routingKey,
                                     Object messageEvent) {
        // 这里直接调用带 delay 的方法，delay 传 null 或 0
        sendMqMessage(rabbitTemplate, exchange, routingKey, messageEvent, null);
    }

    /**
     * 延迟交换机
     *
     */
    public static void sendMqMessage(RabbitTemplate rabbitTemplate,
                                     String exchange,
                                     String routingKey,
                                     Object messageEvent,
                                     Integer delayTime) {
        // 初始化自定义的 CorrelationData，多传一个 delayTime
        RetryCorrelationData cd = new RetryCorrelationData(
                UUID.randomUUID().toString(),
                messageEvent,
                exchange,
                routingKey,
                delayTime,
                3
                // ⭐ 新增字段
        );

        sendWithRetry(rabbitTemplate, cd);
    }
    /**
     * 发送消息并绑定消息回调
     */
    public static void sendWithRetry(RabbitTemplate rabbitTemplate, RetryCorrelationData cd) {
        // 绑定回调
        cd.getFuture().addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable ex) {
                log.error("❌ 发送异常: {}", ex.getMessage());
                // 进行消息重发
                handleRetry(rabbitTemplate, cd);
            }

            @Override
            public void onSuccess(CorrelationData.Confirm result) {
                if (result.isAck()) {
                    log.info("收到ConfirmCallback ack 消息发送成功");
                } else {
                    log.error("收到ConfirmCallback ack 消息发送失败！reason：{}", result.getReason());
                    //进行消息重发
                    handleRetry(rabbitTemplate, cd);
                }
            }
        });

        log.info("🚀 正在发送消息，ID: {}, delayTime: {}", cd.getId(), cd.getDelayTime());

        Integer delayTime = cd.getDelayTime();
        // ⭐ 这里判断有没有传延迟时间：
        if (delayTime != null && delayTime > 0) {
            // 有延迟 → 延迟死信队列消息
            rabbitTemplate.convertAndSend(
                    cd.getDeadExchange(),
                    cd.getDeadRoutingKey(),
                    cd.getMessage(),
                    message -> {
                        message.getMessageProperties().setDelay(delayTime);
                        return message;
                    },
                    cd  // 带上 CorrelationData 用于 confirm 回调
            );
        } else {
            // 无延迟 → 死信队列消息
            rabbitTemplate.convertAndSend(
                    cd.getDeadExchange(),
                    cd.getDeadRoutingKey(),
                    cd.getMessage(),
                    cd
            );
        }
    }

    // 重试处理逻辑
    public static void handleRetry(RabbitTemplate rabbitTemplate, RetryCorrelationData cd) {
        if (cd.getRetryCount() < cd.getMaxRetries()) {
            cd.setRetryCount(cd.getRetryCount() + 1);

            // 延迟 2 分钟后执行重发
            retryExecutor.schedule(() -> {
                log.info("🔄 执行第 {} 次重试发送...", cd.getRetryCount());
                sendWithRetry(rabbitTemplate, cd);
            }, 2, TimeUnit.MINUTES);

        } else {
            log.error("重试次数耗尽，消息发送最终失败。请记录到死信表或人工处理。ID: {}", cd.getId());
            log.info("发送失败的数据为{}", cd.getMessage());
        }
    }
}
