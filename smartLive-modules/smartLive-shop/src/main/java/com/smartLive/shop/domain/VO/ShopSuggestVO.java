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

    /** 待处理评价数量 */
    private Integer pendingReviewCount = 0;

    /** 差评数量 */
    private Integer badReviewCount = 0;

    /** 复购率 */
    private BigDecimal repurchaseRate = BigDecimal.ZERO;

    /** 滞销商品列表 */
    private List<ProductSalesVO> slowProducts = new ArrayList<>();

    /** 差评关键词 */
    private List<String> badReviewKeywords = new ArrayList<>();
}
