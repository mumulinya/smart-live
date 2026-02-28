package com.smartLive.common.core.constant.mq;

/**
 * 订单与支付模�?MQ 常量
 */
public interface OrderMqConstants {
    // 订单交换机名�?
    String ORDER_EXCHANGE_NAME = "order.direct";
    String ORDER_SECKILL_QUEUE = "order.seckill.queue";
    String ORDER_SECKILL_ROUTING = "order.seckill.voucher";
    String ORDER_BUY_QUEUE = "order.buy.queue";
    String ORDER_BUY_ROUTING = "order.buy.voucher";
    
    // 订单死信交换�?
    String ORDER_DEAD_LETTER_EXCHANGE_NAME = "order.dead.letter.direct";
    String ORDER_DEAD_LETTER_QUEUE = "order.dead.letter.queue";
    String ORDER_DEAD_LETTER_ROUTING = "order.dead.letter.routing";

    // 支付延迟交换�?
    String ORDER_DELAY_EXCHANGE_NAME = "order.delay.direct";
    String ORDER_DELAY_QUEUE = "order.delay.queue";
    String ORDER_DELAY_ROUTING = "order.delay.routing";
    Integer DELAY_TIME = 15 * 60000; // 15分钟 = 900000 毫秒

    // 支付记录延迟交换�?(充值超时自动取�?
    String PAY_DELAY_EXCHANGE_NAME = "pay.delay.direct";
    String PAY_DELAY_QUEUE = "pay.delay.queue";
    String PAY_DELAY_ROUTING = "pay.delay.routing";
    Integer PAY_DELAY_TIME = 15 * 60000; // 15分钟
}
