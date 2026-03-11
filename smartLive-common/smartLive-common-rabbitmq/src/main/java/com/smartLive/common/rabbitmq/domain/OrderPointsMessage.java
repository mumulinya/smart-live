package com.smartLive.common.rabbitmq.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单核销积分奖励消息
 * 订单核销成功后，发送此消息给积分模块，用于异步增加用户积分
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPointsMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 实付金额（单位：分） */
    private BigDecimal payAmount;
}
