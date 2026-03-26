package com.smartLive.wallet.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet transaction view object.
 */
@Data
public class WalletTransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date time;

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
