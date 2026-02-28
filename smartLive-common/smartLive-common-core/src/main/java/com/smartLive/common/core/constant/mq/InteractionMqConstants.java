package com.smartLive.common.core.constant.mq;

/**
 * 互动模块 MQ 常量
 */
public interface InteractionMqConstants {
    // 数据同步交换�?
    String INTERACTION_SYNC_EXCHANGE_NAME = "interaction.sync.direct";
    String INTERACTION_SYNC_QUEUE = "interaction.sync.queue";
    String INTERACTION_SYNC_ROUTING = "interaction.sync.trigger";

    // 推送数据交换机
    String INTERACT_FEED_EXCHANGE_NAME = "interact.feed.topic";
    // 普通数据推送队�?
    String INTERACT_FEED_QUEUE = "interact.feed.queue";
    // 数据推�?
    String INTERACT_FEED_ROUTING = "interact.feed";
}
