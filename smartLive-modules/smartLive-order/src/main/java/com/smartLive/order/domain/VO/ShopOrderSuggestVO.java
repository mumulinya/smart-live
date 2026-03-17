package com.smartLive.order.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 店铺订单经营建议视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOrderSuggestVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 近一周订单数 */
    private Integer weekOrders = 0;

    /** 热销商品列表 */
    private List<ProductSalesVO> hotProducts = new ArrayList<>();

    /** 滞销商品列表 */
    private List<ProductSalesVO> slowProducts = new ArrayList<>();
}
