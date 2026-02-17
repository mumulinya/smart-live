package com.smartLive.wallet.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 统一下单请求DTO
 *
 * @author smartLive
 */
@Data
public class UnifiedPayDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型: recharge(充值), order(订单) */
    private String bizType;

    /** 业务ID: 充值单号 或 订单号 */
    private String bizId;

    /** 支付金额(元) - 充值场景必传 */
    private BigDecimal amount;

    /** 支付方式: wechat */
    private String payMethod;

    /** 终端类型: app, h5, miniapp */
    private String appType;
}
