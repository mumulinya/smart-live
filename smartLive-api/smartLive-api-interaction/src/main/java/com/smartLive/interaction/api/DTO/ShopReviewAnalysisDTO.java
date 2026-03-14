package com.smartLive.interaction.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopReviewAnalysisDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal avgScore = BigDecimal.ZERO;

    private Integer badReviewCount = 0;
}