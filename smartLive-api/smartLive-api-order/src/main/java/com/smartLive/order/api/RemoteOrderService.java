package com.smartLive.order.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.order.api.DTO.ProductSoldDTO;
import com.smartLive.order.api.DTO.ShopOrderAnalysisDTO;
import com.smartLive.order.api.DTO.ShopOrderSuggestDTO;
import com.smartLive.order.api.factory.RemoteOrderFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

@FeignClient(contextId = "remoteOrderService", value = ServiceNameConstants.ORDER_SERVICE, fallbackFactory = RemoteOrderFallbackFactory.class)
public interface RemoteOrderService {

    @GetMapping("/inner/order/getOrderCount/{userId}")
    Integer getOrderCount(@PathVariable("userId") Long userId);

    @GetMapping("/inner/order/getOrderTotal")
    Integer getOrderTotal();

    @PutMapping("/inner/order/updateOrderReviewStatus/{orderId}")
    Integer updateOrderReviewStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam(value = "reviewId", required = false) Long reviewId,
                                    @RequestParam(value = "reviewTime", required = false) Date reviewTime);

    @PutMapping("/inner/order/paySuccess/{orderId}/{payType}")
    Integer paySuccess(@PathVariable("orderId") Long orderId, @PathVariable("payType") Integer payType);

    @GetMapping("/inner/order/count/product/sold")
    List<ProductSoldDTO> countProductSold();

    @GetMapping("/inner/order/count/week/orders")
    Integer countWeekOrders(@RequestParam("shopId") Long shopId);

    @GetMapping("/inner/order/analysis/{shopId}")
    ShopOrderAnalysisDTO getShopOrderAnalysis(@PathVariable("shopId") Long shopId,
                                              @RequestParam("startTime") String startTime,
                                              @RequestParam("endTime") String endTime);

    @GetMapping("/inner/order/suggest/{shopId}")
    ShopOrderSuggestDTO getShopOrderSuggest(@PathVariable("shopId") Long shopId,
                                            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange);
}