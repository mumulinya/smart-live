package com.smartLive.shop.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品销量视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;

    private String productName;

    private Long salesCount;
}