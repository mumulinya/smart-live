package com.smartLive.common.rabbitmq.domain;

/**
 * MQ发送模式。
 */
public enum MqSendMode {
    /**
     * 默认模式：
     * 当前线程负责首次发送；
     * confirm失败后的重试在异步线程中完成；
     * 最终失败只记录日志，不再回抛给调用方。
     */
    ASYNC_RETRY,

    /**
     * 严格模式：
     * 当前线程同步等待confirm结果；
     * 所有重试都在当前线程执行；
     * 最终失败会抛异常给调用方。
     */
    SYNC_RETRY_THROW
}
