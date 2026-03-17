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

    /** 分析记录ID */
    private Long id;

    /** 订单总数 */
    private Integer totalOrders = 0;

    /** 订单总收入 */
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    /** 平均评分 */
    private BigDecimal avgScore = BigDecimal.ZERO;

    /** 差评数量 */
    private Integer badReviewCount = 0;

    /** 热销商品列表 */
    private List<ProductSalesVO> hotProducts = new ArrayList<>();

    /** 滞销商品列表 */
    private List<ProductSalesVO> slowProducts = new ArrayList<>();
}
