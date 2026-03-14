package com.smartLive.order.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOrderSuggestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer weekOrders = 0;

    private List<ProductSalesDTO> hotProducts = new ArrayList<>();

    private List<ProductSalesDTO> slowProducts = new ArrayList<>();
}