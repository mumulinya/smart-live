package com.smartLive.interaction.api.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopReviewSuggestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer badReviewCount = 0;

    private List<BadReviewDTO> badReviewList = new ArrayList<>();
}