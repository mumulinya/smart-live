package com.smartLive.order.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 店铺订单分析视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOrderAnalysisVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 订单总数 */
    private Integer totalOrders = 0;

    /** 订单总收入 */
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    /** 复购人数 */
    private Integer repurchaseCount = 0;

    /** 热销商品列表 */
    private List<ProductSalesVO> hotProducts = new ArrayList<>();
}
