package com.smartLive.order.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
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
    public VoucherOrder selectVoucherOrderById(Long id);

    /**
     * 查询优惠券订单表列表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 优惠券订单表集合
     */
    public List<VoucherOrder> selectVoucherOrderList(VoucherOrder voucherOrder);

    /**
     * 新增优惠券订单表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 结果
     */
    public int insertVoucherOrder(VoucherOrder voucherOrder);

    /**
     * 修改优惠券订单表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 结果
     */
    public int updateVoucherOrder(VoucherOrder voucherOrder);

    /**
     * 批量删除优惠券订单表
     * 
     * @param ids 需要删除的优惠券订单表主键集合
     * @return 结果
     */
    public int deleteVoucherOrderByIds(Long[] ids);

    /**
     * 删除优惠券订单表信息
     * 
     * @param id 优惠券订单表主键
     * @return 结果
     */
    public int deleteVoucherOrderById(Long id);

    /**
     * 秒杀优惠券
     * @param voucherId
     * @return
     */
    Result seckillVoucher(Long voucherId);

    /**
     *实现一人一单
     * @param voucher
     * @return
     */
    void createVoucherOrder(VoucherOrder voucher);

    /**
     * 购买优惠券
     * @param voucherId
     * @return
     */
    Result buyVoucher(Long voucherId);
}
