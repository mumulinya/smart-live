package com.smartLive.order.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.order.domain.Order;

/**
 * 订单表Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface OrderMapper extends BaseMapper<Order>
{
    /**
     * 查询订单表
     * 
     * @param id 订单表主键
     * @return 订单表
     */
    public Order selectOrderById(Long id);

    /**
     * 查询订单表列表
     * 
     * @param order 订单表
     * @return 订单表集合
     */
    public List<Order> selectOrderList(Order order);

    /**
     * 新增订单表
     * 
     * @param order 订单表
     * @return 结果
     */
    public int insertOrder(Order order);

    /**
     * 修改订单表
     * 
     * @param order 订单表
     * @return 结果
     */
    public int updateOrder(Order order);

    /**
     * 删除订单表
     * 
     * @param id 订单表主键
     * @return 结果
     */
    public int deleteOrderById(Long id);

    /**
     * 批量删除订单表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderByIds(Long[] ids);
}