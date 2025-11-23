package com.smartLive.common.core.domain;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import lombok.Data;

@Data
public class RetryCorrelationData extends CorrelationData {
    private Object message;
    private String exchange;
    private String routingKey;
    private Integer delayTime;   // ⭐ 新增字段

    private int retryCount = 0;
    private int maxRetries = 3;

    public RetryCorrelationData(String id, Object message, String exchange, String routingKey, Integer delayTime) {
        super(id);
        this.message = message;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.delayTime = delayTime;
    }
}
