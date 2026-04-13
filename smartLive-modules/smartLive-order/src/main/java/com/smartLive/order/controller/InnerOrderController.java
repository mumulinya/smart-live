package com.smartLive.order.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.order.api.DTO.OrderDTO;
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

/**
 * 订单内部接口控制器，供内部服务调用。
 */
@RestController
@RequestMapping("/inner/order")
public class InnerOrderController extends BaseController {
    @Autowired
    private IOrderService orderService;

    /**
     * 获取用户订单数量。
     *
     * @param userId 用户ID
     * @return 订单数量
     */
    @GetMapping("/getOrderCount/{userId}")
    Integer getCommentCount(@PathVariable("userId") Long userId) {
        return orderService.getOrderCount(userId);
    }

    /**
     * 获取订单总数量。
     *
     * @return 订单数量
     */
    @GetMapping("/getOrderTotal")
    Integer getOrderTotal() {
        return orderService.getOrderTotal();
    }

    /**
     * 更新订单评价状态。
     *
     * @param orderId 订单ID
     * @param reviewId 评价ID
     * @param reviewTime 评价时间
     * @return 影响行数
     */
    @PutMapping("/updateOrderReviewStatus/{orderId}")
    Integer updateOrderReviewStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam(value = "reviewId", required = false) Long reviewId,
                                    @RequestParam(value = "reviewTime", required = false) java.util.Date reviewTime) {
        return orderService.updateOrderReviewStatus(orderId, reviewId, reviewTime);
    }

    /**
     * 支付成功回调处理。
     *
     * @param orderId 订单ID
     * @param payType 支付方式
     * @return 影响行数
     */
    @PutMapping("/paySuccess/{orderId}/{payType}")
    Integer paySuccess(@PathVariable("orderId") Long orderId, @PathVariable("payType") Integer payType) {
        return orderService.paySuccess(orderId, payType);
    }

    /**
     * 第三方退款成功后的内部确认。
     *
     * @param orderId 订单ID
     * @return 影响结果
     */
    @PutMapping("/confirmRefundSuccess/{orderId}")
    Integer confirmRefundSuccess(@PathVariable("orderId") Long orderId) {
        return orderService.confirmRefundSuccess(orderId);
    }

    /**
     * 查询订单内部 DTO，供支付成功后的统计消息补全使用。
     *
     * @param orderId 订单ID
     * @return 订单DTO
     */
    @GetMapping("/getOrderById/{orderId}")
    public OrderDTO getOrderById(@PathVariable("orderId") Long orderId) {
        return orderService.getOrderDTOById(orderId);
    }

    /**
     * 统计商品销量。
     *
     * @return 商品销量列表
     */
    @GetMapping("/count/product/sold")
    public List<ProductSoldVO> countProductSold() {
        return orderService.countProductSold();
    }

    /**
     * 统计店铺近一周核销订单数。
     *
     * @param shopId 店铺ID
     * @return 订单数量
     */
    @GetMapping("/count/week/orders")
    public Integer countWeekOrders(@RequestParam("shopId") Long shopId) {
        return orderService.countWeekOrders(shopId);
    }

    /**
     * 获取店铺订单分析数据。
     *
     * @param shopId 店铺ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 店铺订单分析
     */
    @GetMapping("/analysis/{shopId}")
    public ShopOrderAnalysisVO getShopOrderAnalysis(@PathVariable("shopId") Long shopId,
                                                    @RequestParam("startTime") String startTime,
                                                    @RequestParam("endTime") String endTime) {
        return orderService.getShopOrderAnalysis(shopId, startTime, endTime);
    }

    /**
     * 获取店铺经营建议数据。
     *
     * @param shopId 店铺ID
     * @param timeRange 时间范围标识
     * @return 店铺经营建议
     */
    @GetMapping("/suggest/{shopId}")
    public ShopOrderSuggestVO getShopOrderSuggest(@PathVariable("shopId") Long shopId,
                                                  @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        return orderService.getShopOrderSuggest(shopId, timeRange);
    }

    /**
     * 获取店铺复购率。
     *
     * @param shopId 店铺ID
     * @param timeRange 时间范围标识
     * @return 复购率
     */
    @GetMapping("/repurchase-rate/{shopId}")
    public java.math.BigDecimal getShopRepurchaseRate(@PathVariable("shopId") Long shopId,
                                                       @RequestParam(value = "timeRange", required = false) String timeRange) {
        return orderService.getShopRepurchaseRate(shopId, timeRange);
    }
}
