package com.smartLive.wallet.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付业务处理结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBusinessResult {

    private boolean processed;

    private String bizType;

    private Long orderId;

    private Long userId;

    private Integer payType;

    public boolean shouldPublishOrderPaidStats() {
        return processed && "order".equals(bizType) && orderId != null;
    }
}
