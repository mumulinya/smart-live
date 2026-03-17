package com.smartLive.shop.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopSuggestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pendingReviewCount = 0;

    private Integer badReviewCount = 0;

    private java.math.BigDecimal repurchaseRate = java.math.BigDecimal.ZERO;

    private List<ProductSalesDTO> slowProducts = new ArrayList<>();

    private List<String> badReviewKeywords = new ArrayList<>();
}
