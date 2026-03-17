package com.smartLive.ai.domain.DTO;

import com.smartLive.shop.api.DTO.ShopSuggestDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * 商家聊天数据传输对象。
 */
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

    private String timeRange;

    private Long analysisRecordId;

    private ShopSuggestDTO shopSuggestData;
}
