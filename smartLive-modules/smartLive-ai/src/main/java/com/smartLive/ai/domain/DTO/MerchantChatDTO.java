package com.smartLive.ai.domain.DTO;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantChatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sessionId;

    private Long shopId;

    private String type;

    private String message;

    private String rawMessage;

    private String instruction;

    private Long productId;

    private Long reviewId;

    private String dateRange;
}