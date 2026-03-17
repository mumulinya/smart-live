package com.smartLive.shop.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 店铺经营建议视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopSuggestVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer pendingReviewCount = 0;

    private Integer badReviewCount = 0;

    private BigDecimal repurchaseRate = BigDecimal.ZERO;

    private List<ProductSalesVO> slowProducts = new ArrayList<>();

    private List<String> badReviewKeywords = new ArrayList<>();
}
