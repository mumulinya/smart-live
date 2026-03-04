package com.smartLive.common.core.constant.mq;

/**
 * 商品模块 MQ 常量
 */
public interface ProductMqConstants {

    /**
     * 扣减库存交换机
     */
    String PRODUCT_STOCK_EXCHANGE = "product.stock.direct.exchange";

    /**
     * 扣减库存队列
     */
    String PRODUCT_STOCK_DEDUCT_QUEUE = "product.stock.deduct.queue";

    /**
     * 扣减库存路由键
     */
    String PRODUCT_STOCK_DEDUCT_ROUTING_KEY = "product.stock.deduct";

    /**
     * 商品死信交换机
     */
    String PRODUCT_DLX_EXCHANGE = "product.dlx.direct.exchange";

    /**
     * 商品死信队列
     */
    String PRODUCT_DLQ_QUEUE = "product.dlq.queue";

    /**
     * 商品死信路由键
     */
    String PRODUCT_DLQ_ROUTING_KEY = "product.dlq";
}
