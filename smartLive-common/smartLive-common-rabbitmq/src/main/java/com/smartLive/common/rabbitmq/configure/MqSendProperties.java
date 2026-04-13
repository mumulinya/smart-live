package com.smartLive.common.rabbitmq.configure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQ发送重试配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartlive.mq.send")
public class MqSendProperties {

    /**
     * 默认最大重试次数。
     */
    private int maxRetries = 3;

    /**
     * 重试间隔，单位：秒。
     */
    private long retryIntervalSeconds = 2L;

    /**
     * confirm 等待超时，单位：秒。
     */
    private long confirmTimeoutSeconds = 5L;
}
