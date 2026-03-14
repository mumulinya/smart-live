package com.smartLive.shop.domain.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopSuggestVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer weekOrders = 0;

    private Integer badReviewCount = 0;

    private List<BadReviewVO> badReviewList = new ArrayList<>();

    private List<ProductSalesVO> hotProducts = new ArrayList<>();

    private List<ProductSalesVO> slowProducts = new ArrayList<>();
}