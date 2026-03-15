package com.smartLive.order.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.order.domain.VO.ProductSoldVO;
import com.smartLive.order.domain.VO.ShopOrderAnalysisVO;
import com.smartLive.order.domain.VO.ShopOrderSuggestVO;
import com.smartLive.order.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inner/order")
public class InnerOrderController extends BaseController {
    @Autowired
    private IOrderService orderService;

    @GetMapping("/getOrderCount/{userId}")
    Integer getCommentCount(@PathVariable("userId") Long userId) {
        return orderService.getOrderCount(userId);
    }

    @GetMapping("/getOrderTotal")
    Integer getOrderTotal() {
        return orderService.getOrderTotal();
    }

    @PutMapping("/updateOrderReviewStatus/{orderId}")
    Integer updateOrderReviewStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam(value = "reviewId", required = false) Long reviewId,
                                    @RequestParam(value = "reviewTime", required = false) java.util.Date reviewTime) {
        return orderService.updateOrderReviewStatus(orderId, reviewId, reviewTime);
    }

    @PutMapping("/paySuccess/{orderId}/{payType}")
    Integer paySuccess(@PathVariable("orderId") Long orderId, @PathVariable("payType") Integer payType) {
        return orderService.paySuccess(orderId, payType);
    }

    @GetMapping("/count/product/sold")
    public List<ProductSoldVO> countProductSold() {
        return orderService.countProductSold();
    }

    @GetMapping("/count/week/orders")
    public Integer countWeekOrders(@RequestParam("shopId") Long shopId) {
        return orderService.countWeekOrders(shopId);
    }

    @GetMapping("/analysis/{shopId}")
    public ShopOrderAnalysisVO getShopOrderAnalysis(@PathVariable("shopId") Long shopId,
                                                    @RequestParam("startTime") String startTime,
                                                    @RequestParam("endTime") String endTime) {
        return orderService.getShopOrderAnalysis(shopId, startTime, endTime);
    }

    @GetMapping("/suggest/{shopId}")
    public ShopOrderSuggestVO getShopOrderSuggest(@PathVariable("shopId") Long shopId,
                                                  @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        return orderService.getShopOrderSuggest(shopId, timeRange);
    }
}