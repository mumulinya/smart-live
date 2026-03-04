package com.smartLive.common.core.constant.mq;

/**
 * è®¢å�•ä¸Žæ”¯ä»˜æ¨¡å�?MQ å¸¸é‡�
 */
public interface OrderMqConstants {
    // è®¢å�•äº¤æ�¢æœºå��ç§?
    String ORDER_EXCHANGE_NAME = "order.direct";
    String ORDER_SECKILL_QUEUE = "order.seckill.queue";
    String ORDER_SECKILL_ROUTING = "order.seckill.voucher";
    String ORDER_BUY_QUEUE = "order.buy.queue";
    String ORDER_BUY_ROUTING = "order.buy.voucher";

    // 删除/退单回滚
    String ORDER_CANCEL_EXCHANGE_NAME = "order.cancel.direct";
    String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    String ORDER_CANCEL_ROUTING = "order.cancel.routing";
    
    // è®¢å�•æ­»ä¿¡äº¤æ�¢æœ?
    String ORDER_DEAD_LETTER_EXCHANGE_NAME = "order.dead.letter.direct";
    String ORDER_DEAD_LETTER_QUEUE = "order.dead.letter.queue";
    String ORDER_DEAD_LETTER_ROUTING = "order.dead.letter.routing";

    // æ”¯ä»˜å»¶è¿Ÿäº¤æ�¢æœ?
    String ORDER_DELAY_EXCHANGE_NAME = "order.delay.direct";
    String ORDER_DELAY_QUEUE = "order.delay.queue";
    String ORDER_DELAY_ROUTING = "order.delay.routing";
    Integer DELAY_TIME = 15 * 60000; // 15åˆ†é’Ÿ = 900000 æ¯«ç§’

    // æ”¯ä»˜è®°å½•å»¶è¿Ÿäº¤æ�¢æœ?(å……å€¼è¶…æ—¶è‡ªåŠ¨å�–æ¶?
    String PAY_DELAY_EXCHANGE_NAME = "pay.delay.direct";
    String PAY_DELAY_QUEUE = "pay.delay.queue";
    String PAY_DELAY_ROUTING = "pay.delay.routing";
    String PAY_DEAD_LETTER_EXCHANGE_NAME = "pay.dead.letter.direct";
    String PAY_DEAD_LETTER_QUEUE = "pay.dead.letter.queue";
    String PAY_DEAD_LETTER_ROUTING = "pay.dead.letter.routing";
    Integer PAY_DELAY_TIME = 15 * 60000; // 15åˆ†é’Ÿ
}
