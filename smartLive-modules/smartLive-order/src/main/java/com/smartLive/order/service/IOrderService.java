package com.smartLive.order.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.order.domain.VO.OrderVO;
import com.smartLive.order.domain.Order;

/**
 * 订单表Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IOrderService  extends IService<Order>
{
    /**
     * 查询订单表
     * 
     * @param id 订单表主键
     * @return 订单表
     */
     Order selectOrderById(Long id);

    /**
     * 查询订单表列表
     * 
     * @param order 订单表
     * @return 订单表集合
     */
     List<Order> selectOrderList(Order order);

    /**
     * 新增订单表
     * 
     * @param order 订单表
     * @return 结果
     */
     int insertOrder(Order order);

    /**
     * 修改订单表
     * 
     * @param order 订单表
     * @return 结果
     */
     int updateOrder(Order order);

    /**
     * 批量删除订单表
     * 
     * @param ids 需要删除的订单表主键集合
     * @return 结果
     */
     int deleteOrderByIds(Long[] ids);

    /**
     * 删除订单表信息
     * 
     * @param id 订单表主键
     * @return 结果
     */
     int deleteOrderById(Long id);

    /**
     *实现一人一单
     * @param order
     * @return
     */
    void createOrder(Order order);

    /**
     * 获取当前用户订单列表
     * @return
     */
    List<OrderVO> queryMyOrderList(Order order,Integer current);

    /**
     * 支付订单
     * @param id
     * @param
     * @return
     */
    Integer pay(Long id );

    /**
     * 取消订单
     * @param id
     * @param
     * @return
     */
    Integer cancel(Long id);

    /**
     * 退款订单
     * @param id
     * @param
     * @return
     */
    Integer refund(Long id);

    /**
     * 使用订单
     * @param id
     * @param
     * @return
     */
    Integer use(Long id);

    /**
     * 获取订单数量
     * @param userId
     * @return
     */
    Integer getOrderCount(Long userId);
    
    /**
     * 获取订单总数
     * @return
     */
    Integer getOrderTotal();
    
    /**
     * 根据id获取订单详情
     * @param id
     * @return
     */
    OrderVO getOrderById(Long id);
    
    /**
     * 修改订单评价状态
     * @param orderId
     * @return
     */
    Integer updateOrderReviewStatus(Long orderId);
}