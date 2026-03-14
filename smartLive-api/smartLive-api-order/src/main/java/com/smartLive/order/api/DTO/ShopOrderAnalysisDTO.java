package com.smartLive.order.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOrderAnalysisDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer totalOrders = 0;

    private BigDecimal totalRevenue = BigDecimal.ZERO;

    private Integer repurchaseCount = 0;

    private List<ProductSalesDTO> hotProducts = new ArrayList<>();
}