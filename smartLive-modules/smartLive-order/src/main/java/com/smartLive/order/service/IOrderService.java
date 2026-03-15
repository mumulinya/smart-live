package com.smartLive.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.order.domain.Order;
import com.smartLive.order.domain.VO.OrderVO;
import com.smartLive.order.domain.VO.ProductSalesVO;
import com.smartLive.order.domain.VO.ProductSoldVO;
import com.smartLive.order.domain.VO.ShopOrderAnalysisVO;
import com.smartLive.order.domain.VO.ShopOrderSuggestVO;

import java.util.List;

public interface IOrderService extends IService<Order> {
    Order selectOrderById(Long id);

    List<Order> selectOrderList(Order order);

    int insertOrder(Order order);

    int updateOrder(Order order);

    int deleteOrderByIds(Long[] ids);

    int deleteOrderById(Long id);

    void createOrder(Order order);

    List<OrderVO> queryMyOrderList(Order order, Integer current);

    Integer pay(Long id);

    Integer cancel(Long id);

    Integer refund(Long id);

    Integer use(Long id, Long verifyShopId);

    Integer getOrderCount(Long userId);

    Integer getOrderTotal();

    OrderVO getOrderById(Long id);

    Integer updateOrderReviewStatus(Long orderId, Long reviewId, java.util.Date reviewTime);

    Integer paySuccess(Long orderId, Integer payType);

    String getOrderStatus(Long id);

    List<ProductSoldVO> countProductSold();

    Integer countWeekOrders(Long shopId);

    ShopOrderAnalysisVO getShopOrderAnalysis(Long shopId, String startTime, String endTime);

    ShopOrderSuggestVO getShopOrderSuggest(Long shopId, String timeRange);

    Integer expired(Long id);
}