package com.smartLive.marketing.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.marketing.domain.Voucher;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券Mapper接口
 * 
 * @author ruoyi
 * @date 2025-09-21
 */
public interface VoucherMapper extends BaseMapper<Voucher>
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
     * 删除优惠券
     * 
     * @param id 优惠券主键
     * @return 结果
     */
    public int deleteVoucherById(Long id);

    /**
     * 批量删除优惠券
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVoucherByIds(Long[] ids);

    /**
     * 查询店铺下的优惠券
     * @param shopId
     * @return
     */
    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);

}
