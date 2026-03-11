package com.smartLive.common.rabbitmq.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单退款消息实体类
 * 用于订单取消/退款时通知钱包模块恢复余额
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRefundMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 退款金额（单位：分） */
    private BigDecimal amount;

    /** 支付方式: 1=余额 2=支付宝 3=微信 */
    private Integer payType;
}
