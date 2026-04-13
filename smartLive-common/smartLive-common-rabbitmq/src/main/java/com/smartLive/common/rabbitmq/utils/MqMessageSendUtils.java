package com.smartLive.common.rabbitmq.utils;

import com.smartLive.common.rabbitmq.configure.MqSendProperties;
import com.smartLive.common.rabbitmq.domain.MqSendMode;
import com.smartLive.common.rabbitmq.domain.RetryCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
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
    private final MqSendProperties mqSendProperties;

    @Autowired
    public MqMessageSendUtils(ScheduledExecutorService scheduledExecutorService,
                              RabbitTemplate rabbitTemplate,
                              MqSendProperties mqSendProperties) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.rabbitTemplate = rabbitTemplate;
        this.mqSendProperties = mqSendProperties;
    }

    /**
     * 普通交换机
     */
    public void sendMqMessage(
            String exchange,
            String routingKey,
            Object messageEvent)
    {
        sendMqMessage(exchange, routingKey, messageEvent, MqSendMode.ASYNC_RETRY);
    }

    /**
     * 普通交换机，指定发送模式。
     */
    public void sendMqMessage(
            String exchange,
            String routingKey,
            Object messageEvent,
            MqSendMode sendMode)
    {
        doSend(exchange, routingKey, messageEvent, null, sendMode);
    }

    /**
     * 延迟交换机，默认异步确认重试。
     */
    public void sendMqMessage(
            String exchange,
            String routingKey,
            Object messageEvent,
            Integer delayTime) {
        doSend(exchange, routingKey, messageEvent, delayTime, MqSendMode.ASYNC_RETRY);
    }

    /**
     * 延迟交换机，指定发送模式。
     */
    public void sendMqMessage(
            String exchange,
            String routingKey,
            Object messageEvent,
            Integer delayTime,
            MqSendMode sendMode) {
        doSend(exchange, routingKey, messageEvent, delayTime, sendMode);
    }
    /**
     * 发送消息
     */
    private void doSend(String exchange,
                        String routingKey,
                        Object messageEvent,
                        Integer delayTime,
                        MqSendMode sendMode) {
        RetryCorrelationData cd = buildCorrelationData(
                UUID.randomUUID().toString(),
                messageEvent,
                exchange,
                routingKey,
                delayTime,
                0,
                mqSendProperties.getMaxRetries()
        );
        if (sendMode == MqSendMode.SYNC_RETRY_THROW) {
            sendSyncWithRetry(cd);
            return;
        }
        sendAsyncWithRetry(cd, true);
    }

    /**
     * 异步confirm重试：
     * 首次发送在当前线程完成，confirm失败后的重试在异步线程处理。
     */
    private void sendAsyncWithRetry(RetryCorrelationData cd, boolean propagateSendException) {
        cd.getFuture().whenComplete((confirm, throwable) -> {
            if (throwable != null) {
                scheduleAsyncRetry(cd, throwable);
                return;
            }
            try {
                assertSendSuccess(cd, confirm);
                log.info("MQ消息发送成功，mode={}, messageId={}, retryCount={}",
                        MqSendMode.ASYNC_RETRY, cd.getId(), cd.getRetryCount());
            } catch (RuntimeException ex) {
                scheduleAsyncRetry(cd, ex);
            }
        });

        log.info("MQ消息准备发送，mode={}, messageId={}, retryCount={}, delayTime={}",
                MqSendMode.ASYNC_RETRY, cd.getId(), cd.getRetryCount(), cd.getDelayTime());
        try {
            doConvertAndSend(cd);
        } catch (RuntimeException ex) {
            if (propagateSendException) {
                throw ex;
            }
            scheduleAsyncRetry(cd, ex);
        }
    }

    /**
     * 同步confirm重试：
     * 当前线程负责发送、等待confirm以及全部重试流程；
     * 最终失败直接回抛给调用方。
     */
    private void sendSyncWithRetry(RetryCorrelationData initialCd) {
        RetryCorrelationData currentCd = initialCd;
        while (true) {
            try {
                log.info("MQ消息准备发送，mode={}, messageId={}, retryCount={}, delayTime={}",
                        MqSendMode.SYNC_RETRY_THROW, currentCd.getId(), currentCd.getRetryCount(), currentCd.getDelayTime());
                doConvertAndSend(currentCd);
                CorrelationData.Confirm confirm = currentCd.getFuture().get(mqSendProperties.getConfirmTimeoutSeconds(), TimeUnit.SECONDS);
                assertSendSuccess(currentCd, confirm);
                log.info("MQ消息发送成功，mode={}, messageId={}, retryCount={}",
                        MqSendMode.SYNC_RETRY_THROW, currentCd.getId(), currentCd.getRetryCount());
                return;
            } catch (Exception ex) {
                if (currentCd.getRetryCount() >= currentCd.getMaxRetries()) {
                    log.error("MQ消息同步发送最终失败，messageId={}, exchange={}, routingKey={}",
                            currentCd.getId(), currentCd.getExchange(), currentCd.getRoutingKey(), ex);
                    throw new RuntimeException("MQ发送失败", ex);
                }
                RetryCorrelationData nextCd = createNextRetryCorrelationData(currentCd);
                log.warn("MQ消息同步发送失败，准备执行第{}次重试，messageId={}, exchange={}, routingKey={}",
                        nextCd.getRetryCount(), currentCd.getId(), currentCd.getExchange(), currentCd.getRoutingKey(), ex);
                sleepBeforeRetry();
                currentCd = nextCd;
            }
        }
    }

    private void doConvertAndSend(RetryCorrelationData cd) {
        Integer delayTime = cd.getDelayTime();
        if (delayTime != null && delayTime > 0) {
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

    private void scheduleAsyncRetry(RetryCorrelationData cd, Throwable throwable) {
        if (cd.getRetryCount() >= cd.getMaxRetries()) {
            log.error("MQ消息异步发送最终失败，messageId={}, exchange={}, routingKey={}",
                    cd.getId(), cd.getExchange(), cd.getRoutingKey(), throwable);
            return;
        }
        RetryCorrelationData nextCd = createNextRetryCorrelationData(cd);
        log.warn("MQ消息异步发送失败，准备执行第{}次重试，messageId={}, exchange={}, routingKey={}, reason={}",
                nextCd.getRetryCount(), cd.getId(), cd.getExchange(), cd.getRoutingKey(), throwable.getMessage());
        scheduledExecutorService.schedule(() -> sendAsyncWithRetry(nextCd, false),
                mqSendProperties.getRetryIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void assertSendSuccess(RetryCorrelationData cd, CorrelationData.Confirm confirm) {
        if (confirm == null) {
            throw new RuntimeException("未收到MQ confirm结果");
        }
        if (!confirm.isAck()) {
            throw new RuntimeException("收到Confirm nack，reason=" + confirm.getReason());
        }
        ReturnedMessage returned = cd.getReturned();
        if (returned != null) {
            throw new RuntimeException(String.format(
                    "消息路由失败，replyCode=%s, replyText=%s, exchange=%s, routingKey=%s",
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey()
            ));
        }
    }

    private RetryCorrelationData buildCorrelationData(String id,
                                                      Object messageEvent,
                                                      String exchange,
                                                      String routingKey,
                                                      Integer delayTime,
                                                      int retryCount,
                                                      int maxRetries) {
        RetryCorrelationData cd = (delayTime != null && delayTime > 0)
                ? new RetryCorrelationData(id, messageEvent, exchange, routingKey, delayTime, maxRetries)
                : new RetryCorrelationData(id, messageEvent, exchange, routingKey, maxRetries);
        cd.setRetryCount(retryCount);
        return cd;
    }

    private RetryCorrelationData createNextRetryCorrelationData(RetryCorrelationData cd) {
        return buildCorrelationData(
                cd.getId(),
                cd.getMessage(),
                cd.getExchange(),
                cd.getRoutingKey(),
                cd.getDelayTime(),
                cd.getRetryCount() + 1,
                cd.getMaxRetries()
        );
    }

    private void sleepBeforeRetry() {
        try {
            TimeUnit.SECONDS.sleep(mqSendProperties.getRetryIntervalSeconds());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("MQ发送重试被中断", ex);
        }
    }
}
