package com.smartLive.wallet.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Set pay password request.
 */
@Data
public class WalletPasswordSetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String password;
}
