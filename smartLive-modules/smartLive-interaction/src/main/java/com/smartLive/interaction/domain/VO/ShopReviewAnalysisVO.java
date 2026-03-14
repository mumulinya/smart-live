package com.smartLive.interaction.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopReviewAnalysisVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal avgScore = BigDecimal.ZERO;

    private Integer badReviewCount = 0;
}