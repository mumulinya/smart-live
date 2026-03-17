package com.smartLive.shop.service;

import com.smartLive.shop.domain.VO.ShopSuggestVO;

/**
 * 店铺经营建议服务接口。
 */
public interface IShopSuggestService {

    /**
     * 获取店铺经营建议。
     */
    ShopSuggestVO getShopSuggest(Long shopId);

    /**
     * 获取店铺经营建议。
     */
    ShopSuggestVO getShopSuggestInner(Long shopId);
}
