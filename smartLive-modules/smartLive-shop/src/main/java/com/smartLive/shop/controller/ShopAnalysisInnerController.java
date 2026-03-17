package com.smartLive.shop.controller;

import com.smartLive.shop.domain.VO.ShopAnalysisVO;
import com.smartLive.shop.service.IShopAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inner/shop/analysis")
public class ShopAnalysisInnerController {

    @Autowired
    private IShopAnalysisService shopAnalysisService;

    @GetMapping
    public ShopAnalysisVO getShopAnalysis(@RequestParam("shopId") Long shopId,
                                          @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        return shopAnalysisService.getShopAnalysis(shopId, timeRange);
    }

    @GetMapping("/record/{analysisRecordId}")
    public ShopAnalysisVO getShopAnalysisRecord(@PathVariable("analysisRecordId") Long analysisRecordId,
                                                @RequestParam("shopId") Long shopId) {
        return shopAnalysisService.getShopAnalysisRecord(analysisRecordId, shopId);
    }
}
