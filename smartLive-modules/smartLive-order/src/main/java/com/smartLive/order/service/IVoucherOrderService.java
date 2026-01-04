package com.smartLive.order.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.order.domain.VoucherOrder;

/**
 * 优惠券订单表Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IVoucherOrderService  extends IService<VoucherOrder>
{
    /**
     * 查询优惠券订单表
     * 
     * @param id 优惠券订单表主键
     * @return 优惠券订单表
     */
     VoucherOrder selectVoucherOrderById(Long id);

    /**
     * 查询优惠券订单表列表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 优惠券订单表集合
     */
     List<VoucherOrder> selectVoucherOrderList(VoucherOrder voucherOrder);

    /**
     * 新增优惠券订单表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 结果
     */
     int insertVoucherOrder(VoucherOrder voucherOrder);

    /**
     * 修改优惠券订单表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 结果
     */
     int updateVoucherOrder(VoucherOrder voucherOrder);

    /**
     * 批量删除优惠券订单表
     * 
     * @param ids 需要删除的优惠券订单表主键集合
     * @return 结果
     */
     int deleteVoucherOrderByIds(Long[] ids);

    /**
     * 删除优惠券订单表信息
     * 
     * @param id 优惠券订单表主键
     * @return 结果
     */
     int deleteVoucherOrderById(Long id);

    /**
     *实现一人一单
     * @param voucher
     * @return
     */
    void createVoucherOrder(VoucherOrder voucher);

    /**
     * 获取当前用户订单列表
     * @return
     */
    List<VoucherOrder> queryMyVoucherOrderList(Long userId,Integer current);

    /**
     * 支付订单
     * @param id
     * @param
     * @return
     */
    Integer pay(Long id );

    /**
     * 退款订单
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
}
