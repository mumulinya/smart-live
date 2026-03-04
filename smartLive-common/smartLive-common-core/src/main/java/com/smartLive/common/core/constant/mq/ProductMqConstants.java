package com.smartLive.common.core.constant.mq;

/**
 * 商品模块 MQ 常量
 */
public interface ProductMqConstants {

    /**
     * 扣减库存交换机
     */
    String DEDUCT_STOCK_EXCHANGE = "product.stock.direct";

    /**
     * 扣减库存队列
     */
    String DEDUCT_STOCK_QUEUE = "product.stock.deduct.queue";

    /**
     * 扣减库存路由键
     */
    String DEDUCT_STOCK_ROUTING = "product.stock.deduct.routing";

    /**
     * 商品死信交换机
     */
    String PRODUCT_DEAD_LETTER_EXCHANGE_NAME = "product.dead.letter.direct";

    /**
     * 商品死信队列
     */
    String PRODUCT_DEAD_LETTER_QUEUE = "product.dead.letter.queue";

    /**
     * 商品死信路由键
     */
    String PRODUCT_DEAD_LETTER_ROUTING = "product.dead.letter.routing";
}
