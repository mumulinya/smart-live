package com.smartLive.interaction.domain.VO;

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
public class ShopReviewSuggestVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal avgScore = BigDecimal.ZERO;

    private Integer badReviewCount = 0;

    private Integer pendingReviewCount = 0;

    private List<BadReviewVO> badReviewList = new ArrayList<>();
}