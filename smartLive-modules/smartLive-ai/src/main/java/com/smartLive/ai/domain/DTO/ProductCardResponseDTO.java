package com.smartLive.ai.domain.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品卡片结构化响应 DTO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCardResponseDTO extends BaseStructuredResponseDTO {
    private List<RecommendationItemDTO> recommendations;
}
