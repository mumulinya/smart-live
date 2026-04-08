package com.smartLive.common.rabbitmq.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单支付成功后的销量统计消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidStatsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private Long userId;

    private Long sourceId;

    private Long verifyShopId;

    private Integer amount;

    private Integer payType;

    private String messageKey;
}
