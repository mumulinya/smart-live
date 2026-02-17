package com.smartLive.wallet.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 支付状态查询响应VO
 *
 * @author smartLive
 */
@Data
public class PayStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态 0:待支付 1:支付成功 2:支付失败 3:已取消/已过期 */
    private Integer status;

    /** 第三方交易号 (主动查询时返回) */
    private String transactionId;
}
