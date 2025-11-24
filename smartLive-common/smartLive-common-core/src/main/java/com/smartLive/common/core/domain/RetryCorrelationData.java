package com.smartLive.common.core.domain;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import lombok.Data;

/**
 * 消息发送数据类
 */
@Data
public class RetryCorrelationData extends CorrelationData {
    private Object message;
    private String exchange;
    private String routingKey;
    // 死信交换机
    private String deadExchange;
    // 死信路由
    private String deadRoutingKey;
    // 延迟时间
    private Integer delayTime;
    // 重试次数
    private int retryCount = 0;
    // 最大重试次数
    private int maxRetries = 3;

    /**
     * 普通交换机构造方法
     * @param id
     * @param message
     * @param exchange
     * @param routingKey
     */
    public RetryCorrelationData(String id, Object message, String exchange, String routingKey, int maxRetries) {
        super(id);
        this.message = message;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.maxRetries = maxRetries;
    }

    /**
     * 延迟交换机构造方法
     * @param id
     * @param message
     * @param exchange
     * @param routingKey
     * @param delayTime
     */
    public RetryCorrelationData(String id, Object message, String exchange, String routingKey, Integer delayTime, int maxRetries) {
        super(id);
        this.message = message;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.delayTime = delayTime;
        this.maxRetries = maxRetries;
    }
    /**
     * 普通交换机构造方法+死信交换机构造方法
     * @param id
     * @param message
     * @param exchange
     * @param routingKey
     * @param deadExchange
     * @param deadRoutingKey
     */
    public RetryCorrelationData(String id, Object message, String exchange, String routingKey, String deadExchange, String deadRoutingKey, int maxRetries) {
        super(id);
        this.message = message;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.deadExchange = deadExchange;
        this.deadRoutingKey = deadRoutingKey;
        this.maxRetries = maxRetries;
    }
    /**
     * 延迟交换机构造方法+死信交换机构造方法
     * @param id
     * @param message
     * @param exchange
     * @param routingKey
     * @param delayTime
     * @param deadExchange
     * @param deadRoutingKey
     */
    public RetryCorrelationData(String id, Object message, String exchange, String routingKey, Integer delayTime, String deadExchange, String deadRoutingKey, int maxRetries) {
        super(id);
        this.message = message;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.delayTime = delayTime;
        this.deadExchange = deadExchange;
        this.deadRoutingKey = deadRoutingKey;
        this.maxRetries = maxRetries;
    }

    @Override
    public String toString() {
        return "RetryCorrelationData{" +
                "message=" + message +
                ", exchange='" + exchange + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", deadExchange='" + deadExchange + '\'' +
                ", deadRoutingKey='" + deadRoutingKey + '\'' +
                ", delayTime=" + delayTime +
                ", retryCount=" + retryCount +
                ", maxRetries=" + maxRetries +
                '}';
    }
}
