package com.smartLive.order.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSoldVO {
    private Long productId;
    private Long soldCount;
}