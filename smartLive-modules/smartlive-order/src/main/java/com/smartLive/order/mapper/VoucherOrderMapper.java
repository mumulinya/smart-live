package com.smartLive.order.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.order.domain.VoucherOrder;

/**
 * 优惠券订单表Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder>
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
     * 删除优惠券订单表
     * 
     * @param id 优惠券订单表主键
     * @return 结果
     */
    public int deleteVoucherOrderById(Long id);

    /**
     * 批量删除优惠券订单表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVoucherOrderByIds(Long[] ids);
}
