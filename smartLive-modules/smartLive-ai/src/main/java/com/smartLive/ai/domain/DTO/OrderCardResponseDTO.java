package com.smartLive.ai.domain.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 下单成功结构化响应 DTO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCardResponseDTO extends BaseStructuredResponseDTO {
    private String orderId;
}
