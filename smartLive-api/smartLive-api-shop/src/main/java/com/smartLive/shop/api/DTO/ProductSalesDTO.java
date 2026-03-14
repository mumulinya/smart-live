package com.smartLive.shop.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;

    private String productName;

    private Long salesCount;
}