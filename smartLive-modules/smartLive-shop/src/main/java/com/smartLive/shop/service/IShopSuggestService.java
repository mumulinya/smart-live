package com.smartLive.shop.service;

import com.smartLive.shop.domain.VO.ShopSuggestVO;

public interface IShopSuggestService {

    ShopSuggestVO getShopSuggest(Long shopId);

    ShopSuggestVO getShopSuggestInner(Long shopId);
}
