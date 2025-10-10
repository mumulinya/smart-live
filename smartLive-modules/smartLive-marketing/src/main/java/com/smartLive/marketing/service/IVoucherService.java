package com.smartLive.marketing.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.domain.Voucher;

/**
 * 优惠券Service接口
 * 
 * @author ruoyi
 * @date 2025-09-21
 */
public interface IVoucherService extends IService<Voucher>
{
    /**
     * 查询优惠券
     * 
     * @param id 优惠券主键
     * @return 优惠券
     */
    public Voucher selectVoucherById(Long id);

    /**
     * 查询优惠券列表
     * 
     * @param voucher 优惠券
     * @return 优惠券集合
     */
    public List<Voucher> selectVoucherList(Voucher voucher);

    /**
     * 新增优惠券
     * 
     * @param voucher 优惠券
     * @return 结果
     */
    public int insertVoucher(Voucher voucher);

    /**
     * 修改优惠券
     * 
     * @param voucher 优惠券
     * @return 结果
     */
    public int updateVoucher(Voucher voucher);

    /**
     * 批量删除优惠券
     * 
     * @param ids 需要删除的优惠券主键集合
     * @return 结果
     */
    public int deleteVoucherByIds(Long[] ids);

    /**
     * 删除优惠券信息
     * 
     * @param id 优惠券主键
     * @return 结果
     */
    public int deleteVoucherById(Long id);

    /**
     * 根据店铺查询优惠券列表
     * @param shopId
     * @return
     */
    Result queryVoucherOfShop(Long shopId);

    /**
     * 添加秒杀券
     * @param voucher
     */
    void addSeckillVoucher(Voucher voucher);

    /**
     * 查询店铺的优惠券列表
     * @param voucher
     * @return
     */

    List<Voucher> listVoucher();

    /**
     * 查询店铺的秒杀优惠券列表
     * @param voucher
     * @return
     */

    List<Voucher> listSeckillVoucher(Voucher voucher);
}
