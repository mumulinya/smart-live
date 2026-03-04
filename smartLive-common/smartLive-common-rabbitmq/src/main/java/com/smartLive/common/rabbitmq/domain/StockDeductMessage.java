package com.smartLive.common.rabbitmq.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 扣减库存消息实体类
 */
@Data
public class StockDeductMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 订单ID（扣减失败时，用于发送 MQ 通知订单侧取消该订单）
     */
    private Long orderId;

    /**
     * 扣减数量
     */
    private Integer count;
}
