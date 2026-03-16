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
    /**
     * 查询订单详情
     */
    Order selectOrderById(Long id);
    /**
     * 查询订单列表
     */

    List<OrderVO> selectOrderList(Order order);
    /**
     * 新增订单
     */

    int insertOrder(Order order);
    /**
     * 修改订单
     */

    int updateOrder(Order order);
    /**
     * 批量删除订单
     */

    int deleteOrderByIds(Long[] ids);
    /**
     * 删除订单信息
     */

    int deleteOrderById(Long id);
    /**
     * 创建订单
     */

    void createOrder(Order order);
    /**
     * 分页查询我的订单列表
     */

    List<OrderVO> queryMyOrderList(Order order, Integer current);
    /**
     * 订单支付
     */

    Integer pay(Long id);
    /**
     * 取消订单
     */

    Integer cancel(Long id);
    /**
     * 订单退款
     */

    Integer refund(Long id);
    /**
     * 订单核销/使用
     */

    Integer use(Long id, Long verifyShopId);
    /**
     * 获取用户订单总数
     */

    Integer getOrderCount(Long userId);
    /**
     * 获取所有订单总数
     */

    Integer getOrderTotal();
    /**
     * 根据ID获取订单VO对象
     */

    OrderVO getOrderById(Long id);
    /**
     * 更新订单评价状态
     */

    Integer updateOrderReviewStatus(Long orderId, Long reviewId, java.util.Date reviewTime);
    /**
     * 支付成功回调处理
     */

    Integer paySuccess(Long orderId, Integer payType);
    /**
     * 获取订单状态字符串
     */

    String getOrderStatus(Long id);
    /**
     * 统计商品销量
     */

    List<ProductSoldVO> countProductSold();
    /**
     * 统计店铺本周订单数
     */

    Integer countWeekOrders(Long shopId);
    /**
     * 获取店铺订单分析数据
     */

    ShopOrderAnalysisVO getShopOrderAnalysis(Long shopId, String startTime, String endTime);
    /**
     * 获取店铺订单建议数据
     */

    ShopOrderSuggestVO getShopOrderSuggest(Long shopId, String timeRange);
    /**
     * 订单过期作废处理
     */

    Integer expired(Long id);
}