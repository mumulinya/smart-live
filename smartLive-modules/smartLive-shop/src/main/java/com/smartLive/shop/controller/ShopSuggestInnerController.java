package com.smartLive.shop.controller;

import com.smartLive.shop.domain.VO.ShopSuggestVO;
import com.smartLive.shop.service.IShopSuggestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inner/shop/suggest")
public class ShopSuggestInnerController {

    private final IShopSuggestService shopSuggestService;

    public ShopSuggestInnerController(IShopSuggestService shopSuggestService) {
        this.shopSuggestService = shopSuggestService;
    }

    @GetMapping
    public ShopSuggestVO getShopSuggest(@RequestParam("shopId") Long shopId) {
        return shopSuggestService.getShopSuggestInner(shopId);
    }
}
