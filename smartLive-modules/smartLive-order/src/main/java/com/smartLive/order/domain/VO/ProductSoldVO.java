package com.smartLive.order.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品销量统计结果对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSoldVO {
    /** 商品ID */
    private Long productId;
    /** 已售数量 */
    private Long soldCount;
}
