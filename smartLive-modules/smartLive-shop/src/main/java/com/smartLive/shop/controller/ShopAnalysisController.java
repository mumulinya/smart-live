package com.smartLive.shop.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.shop.service.IShopAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺经营分析控制器。
 */
@RestController
@RequestMapping("/shop/analysis")
public class ShopAnalysisController extends BaseController {

    @Autowired
    private IShopAnalysisService shopAnalysisService;

    /**
     * 获取店铺经营分析数据。
     */
    @GetMapping("/{shopId}")
    public AjaxResult getShopAnalysis(@PathVariable("shopId") Long shopId,
                                      @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        return success(shopAnalysisService.getShopAnalysis(shopId, timeRange));
    }
}
