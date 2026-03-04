package com.smartLive.common.core.constant.mq;

/**
 * è®¢å�•ä¸Žæ”¯ä»˜æ¨¡å�?MQ å¸¸é‡�
 */
public interface OrderMqConstants {
    // è®¢å�•äº¤æ�¢æœºå��ç§?
    String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";
    String ORDER_SECKILL_QUEUE = "order.seckill.queue";
    String ORDER_SECKILL_ROUTING_KEY = "order.seckill";
    String ORDER_BUY_QUEUE = "order.buy.queue";
    String ORDER_BUY_ROUTING_KEY = "order.buy";

    // 删除/退单回滚
    String ORDER_CANCEL_EXCHANGE = "order.cancel.direct.exchange";
    String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    String ORDER_CANCEL_ROUTING_KEY = "order.cancel";
    
    // è®¢å�•æ­»ä¿¡äº¤æ�¢æœ?
    String ORDER_DLX_EXCHANGE = "order.dlx.direct.exchange";
    String ORDER_DLQ_QUEUE = "order.dlq.queue";
    String ORDER_DLQ_ROUTING_KEY = "order.dlq";

    // æ”¯ä»˜å»¶è¿Ÿäº¤æ�¢æœ?
    String ORDER_DELAY_EXCHANGE = "order.delay.direct.exchange";
    String ORDER_DELAY_QUEUE = "order.delay.queue";
    String ORDER_DELAY_ROUTING_KEY = "order.delay";
    Integer DELAY_TIME = 15 * 60000; // 15åˆ†é’Ÿ = 900000 æ¯«ç§’

    // æ”¯ä»˜è®°å½•å»¶è¿Ÿäº¤æ�¢æœ?(å……å€¼è¶…æ—¶è‡ªåŠ¨å�–æ¶?
    String PAY_DELAY_EXCHANGE = "pay.delay.direct.exchange";
    String PAY_DELAY_QUEUE = "pay.delay.queue";
    String PAY_DELAY_ROUTING_KEY = "pay.delay";
    String PAY_DLX_EXCHANGE = "pay.dlx.direct.exchange";
    String PAY_DLQ_QUEUE = "pay.dlq.queue";
    String PAY_DLQ_ROUTING_KEY = "pay.dlq";
    Integer PAY_DELAY_TIME = 15 * 60000; // 15åˆ†é’Ÿ
}
