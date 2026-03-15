package com.smartLive.shop.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopAnalysisDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalOrders = 0;

    private BigDecimal totalRevenue = BigDecimal.ZERO;

    private BigDecimal avgScore = BigDecimal.ZERO;

    private Integer badReviewCount = 0;
}