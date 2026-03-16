package com.smartLive.ai.domain.DTO;

import com.smartLive.common.core.exception.ServiceException;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Data
public class MerchantChatRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String shopId;

    private String type;

    private String message;

    private String productId;

    private String reviewId;

    private String timeRange;

    public MerchantChatDTO toMerchantChatDTO() {
        MerchantChatDTO dto = new MerchantChatDTO();
        dto.setSessionId(parseRequiredLong(sessionId, "sessionId"));
        dto.setShopId(parseRequiredLong(shopId, "shopId"));
        dto.setType(StringUtils.hasText(type) ? type.trim() : null);
        dto.setMessage(StringUtils.hasText(message) ? message.trim() : null);
        dto.setRawMessage(dto.getMessage());
        dto.setProductId(parseOptionalLong(productId, "productId"));
        dto.setReviewId(parseOptionalLong(reviewId, "reviewId"));
        dto.setTimeRange(StringUtils.hasText(timeRange) ? timeRange.trim() : null);
        return dto;
    }

    private Long parseRequiredLong(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(fieldName + " cannot be blank");
        }
        return parseLong(value, fieldName);
    }

    private Long parseOptionalLong(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseLong(value, fieldName);
    }

    private Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + " format is invalid");
        }
    }
}