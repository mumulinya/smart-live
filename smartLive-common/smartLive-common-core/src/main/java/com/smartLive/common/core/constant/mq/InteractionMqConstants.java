package com.smartLive.common.core.constant.mq;

/**
 * 互动模块 MQ 常量
 */
public interface InteractionMqConstants {
    // 推送数据交换机
    String INTERACT_FEED_EXCHANGE = "interact.feed.topic.exchange";
    // 普通数据推送队列
    String INTERACT_FEED_QUEUE = "interact.feed.queue";
    // 数据推送路由
    String INTERACT_FEED_ROUTING_KEY = "interact.feed";

    String INTERACTION_DLX_EXCHANGE = "interaction.dlx.direct.exchange";
    String INTERACTION_DLQ_QUEUE = "interaction.dlq.queue";
    String INTERACTION_DLQ_ROUTING_KEY = "interaction.dlq";
}
