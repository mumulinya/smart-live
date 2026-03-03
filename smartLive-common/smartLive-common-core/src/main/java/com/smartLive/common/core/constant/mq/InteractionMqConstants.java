package com.smartLive.common.core.constant.mq;

/**
 * 互动模块 MQ 常量
 */
public interface InteractionMqConstants {
    // 推送数据交换机
    String INTERACT_FEED_EXCHANGE_NAME = "interact.feed.topic";
    // 普通数据推送队列
    String INTERACT_FEED_QUEUE = "interact.feed.queue";
    // 数据推送路由
    String INTERACT_FEED_ROUTING = "interact.feed";

    String INTERACTION_DEAD_LETTER_EXCHANGE_NAME = "interaction.dead.letter.direct";
    String INTERACTION_DEAD_LETTER_QUEUE = "interaction.dead.letter.queue";
    String INTERACTION_DEAD_LETTER_ROUTING = "interaction.dead.letter.routing";
}
