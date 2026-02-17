package com.smartLive.wallet.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Verify pay password request.
 */
@Data
public class WalletPasswordVerifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String password;
}
