package com.smartLive.wallet.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Wallet info view object.
 */
@Data
public class WalletInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal balance;

    private BigDecimal frozenBalance;

    private Boolean hasPayPassword;
}
