package com.smartLive.order.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.order.api.DTO.ProductSoldDTO;
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

    /**
     * 获取订单数量
     *
     * @param userId
     * @return
     */
    @GetMapping("/inner/order/getOrderCount/{userId}")
    Integer getOrderCount(@PathVariable("userId") Long userId);

    /**
     * 获取订单总数
     *
     * @return
     */
    @GetMapping("/inner/order/getOrderTotal")
    Integer getOrderTotal();

    /**
     * 修改订单评论状态
     *
     * @param orderId
     * @param reviewId
     * @param reviewTime
     * @return
     */
    @PutMapping("/inner/order/updateOrderReviewStatus/{orderId}")
    Integer updateOrderReviewStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam(value = "reviewId", required = false) Long reviewId,
                                    @RequestParam(value = "reviewTime", required = false) Date reviewTime);

    /**
     * 支付成功更新订单状态
     *
     * @param orderId 订单ID
     * @param payType 支付方式: 1=余额 2=支付宝 3=微信
     * @return 影响行数
     */
    @PutMapping("/inner/order/paySuccess/{orderId}/{payType}")
    Integer paySuccess(@PathVariable("orderId") Long orderId, @PathVariable("payType") Integer payType);
    /**
     * 统计商品销售数量
     *
     * @return
     */
    @GetMapping("/inner/order/count/product/sold")
    List<ProductSoldDTO> countProductSold();

    /**
     * 统计店铺近 7 天核销订单数
     *
     * @param shopId 店铺 ID
     * @return 订单数
     */
    @GetMapping("/inner/order/count/week/orders")
    Integer countWeekOrders(@RequestParam("shopId") Long shopId);

}

