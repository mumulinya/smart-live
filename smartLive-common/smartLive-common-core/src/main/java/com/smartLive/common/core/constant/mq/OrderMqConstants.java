package com.smartLive.common.core.constant.mq;

/**
 * 订单与支付模块 MQ 常量
 */
public interface OrderMqConstants {
    // 订单交换机各种
    String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";
    String ORDER_SECKILL_QUEUE = "order.seckill.queue";
    String ORDER_SECKILL_ROUTING_KEY = "order.seckill";
    String ORDER_BUY_QUEUE = "order.buy.queue";
    String ORDER_BUY_ROUTING_KEY = "order.buy";

    // 删除/退单回滚
    String ORDER_CANCEL_EXCHANGE = "order.cancel.direct.exchange";
    String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    String ORDER_CANCEL_ROUTING_KEY = "order.cancel";
    
    // 订单死信交换机
    String ORDER_DLX_EXCHANGE = "order.dlx.direct.exchange";
    String ORDER_DLQ_QUEUE = "order.dlq.queue";
    String ORDER_DLQ_ROUTING_KEY = "order.dlq";

    // 支付延迟交换机
    String ORDER_DELAY_EXCHANGE = "order.delay.direct.exchange";
    String ORDER_DELAY_QUEUE = "order.delay.queue";
    String ORDER_DELAY_ROUTING_KEY = "order.delay";
    Integer DELAY_TIME = 15 * 60000; // 15分钟 = 900000 毫秒

    // 支付记录延迟交换机(充值超时自动取消)
    String PAY_DELAY_EXCHANGE = "pay.delay.direct.exchange";
    String PAY_DELAY_QUEUE = "pay.delay.queue";
    String PAY_DELAY_ROUTING_KEY = "pay.delay";
    String PAY_DLX_EXCHANGE = "pay.dlx.direct.exchange";
    String PAY_DLQ_QUEUE = "pay.dlq.queue";
    String PAY_DLQ_ROUTING_KEY = "pay.dlq";
    Integer PAY_DELAY_TIME = 15 * 60000; // 15分钟

    // 订单退款→钱包退款
    String ORDER_REFUND_EXCHANGE = "order.refund.direct.exchange";
    String ORDER_REFUND_QUEUE = "order.refund.queue";
    String ORDER_REFUND_ROUTING_KEY = "order.refund";

    // 订单支付成功后异步统计销量
    String ORDER_PAID_STATS_EXCHANGE = "order.paid.stats.direct.exchange";
    String ORDER_PAID_STATS_QUEUE = "order.paid.stats.queue";
    String ORDER_PAID_STATS_ROUTING_KEY = "order.paid.stats";
}
