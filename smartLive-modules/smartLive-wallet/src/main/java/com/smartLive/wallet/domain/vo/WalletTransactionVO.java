package com.smartLive.wallet.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Wallet transaction view object.
 */
@Data
public class WalletTransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String time;

    private BigDecimal amount;

    /**
     * in or out.
     */
    private String type;

    /**
     * success, pending, failed.
     */
    private String status;
}
