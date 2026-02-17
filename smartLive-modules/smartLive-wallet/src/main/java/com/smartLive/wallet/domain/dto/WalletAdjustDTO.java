package com.smartLive.wallet.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Admin adjust request.
 */
@Data
public class WalletAdjustDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    /**
     * 1 add, 2 deduct.
     */
    private Integer type;

    private BigDecimal amount;

    private String remark;
}
