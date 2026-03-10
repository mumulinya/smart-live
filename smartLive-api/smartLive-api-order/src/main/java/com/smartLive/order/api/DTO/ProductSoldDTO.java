package com.smartLive.order.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSoldDTO {
    private Long productId;
    private Long soldCount;
}