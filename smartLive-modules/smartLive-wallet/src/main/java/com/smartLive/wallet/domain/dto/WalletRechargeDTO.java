package com.smartLive.wallet.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Recharge request.
 */
@Data
public class WalletRechargeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal amount;
}
