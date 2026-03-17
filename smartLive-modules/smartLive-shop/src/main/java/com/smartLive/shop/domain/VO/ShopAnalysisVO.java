package com.smartLive.shop.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 店铺经营分析视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Integer totalOrders = 0;

    private BigDecimal totalRevenue = BigDecimal.ZERO;

    private BigDecimal avgScore = BigDecimal.ZERO;

    private Integer badReviewCount = 0;

    private List<ProductSalesVO> hotProducts = new ArrayList<>();

    private List<ProductSalesVO> slowProducts = new ArrayList<>();
}
