package com.smartLive.common.core.constant.mq;

/**
 * 积分模块 MQ 常量
 */
public interface PointsMqConstants {

    // 积分交换机
    String POINTS_DIRECT_EXCHANGE = "points.direct.exchange";

    // 订单核销奖励积分
    String POINTS_ORDER_QUEUE = "points.order.queue";
    String POINTS_ORDER_ROUTING_KEY = "points.order";
}
