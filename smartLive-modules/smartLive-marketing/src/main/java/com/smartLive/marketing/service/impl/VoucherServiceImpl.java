package com.smartLive.marketing.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.domain.SeckillVoucher;
import com.smartLive.marketing.service.ISeckillVoucherService;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.smartLive.marketing.mapper.VoucherMapper;
import com.smartLive.marketing.domain.Voucher;
import com.smartLive.marketing.service.IVoucherService;

import javax.annotation.Resource;

/**
 * 优惠券Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-09-21
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService
{
    @Autowired
    private VoucherMapper voucherMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RemoteShopService remoteShopService;


    /**
     * 查询优惠券
     * 
     * @param id 优惠券主键
     * @return 优惠券
     */
    @Override
    public Voucher selectVoucherById(Long id)
    {
        return voucherMapper.selectVoucherById(id);
    }

    /**
     * 查询优惠券列表
     * 
     * @param voucher 优惠券
     * @return 优惠券
     */
    @Override
    public List<Voucher> selectVoucherList(Voucher voucher)
    {
        return voucherMapper.selectVoucherList(voucher);
    }

    /**
     * 新增优惠券
     * 
     * @param voucher 优惠券
     * @return 结果
     */
    @Override
    public int insertVoucher(Voucher voucher)
    {
        voucher.setCreateTime(DateUtils.getNowDate());
        return voucherMapper.insertVoucher(voucher);
    }

    /**
     * 修改优惠券
     * 
     * @param voucher 优惠券
     * @return 结果
     */
    @Override
    public int updateVoucher(Voucher voucher)
    {
        voucher.setUpdateTime(DateUtils.getNowDate());
        return voucherMapper.updateVoucher(voucher);
    }

    /**
     * 批量删除优惠券
     * 
     * @param ids 需要删除的优惠券主键
     * @return 结果
     */
    @Override
    public int deleteVoucherByIds(Long[] ids)
    {
        return voucherMapper.deleteVoucherByIds(ids);
    }

    /**
     * 删除优惠券信息
     * 
     * @param id 优惠券主键
     * @return 结果
     */
    @Override
    public int deleteVoucherById(Long id)
    {
        return voucherMapper.deleteVoucherById(id);
    }


    /**
     * 根据店铺查询优惠券列表
     *
     * @param shopId
     * @return
     */
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    /**
     * 添加秒杀券
     *
     * @param voucher
     */
    @Override
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //把秒杀库存写入redis
        String key= RedisConstants.SECKILL_STOCK_KEY + voucher.getId();
        stringRedisTemplate.opsForValue().set(key, voucher.getStock().toString());
    }

    /**
     * 查询店铺的优惠券列表
     *
     * @param voucher
     * @return
     */
    @Override
    public List<Voucher> listVoucher(Voucher voucher) {
        Long shopId = voucher.getShopId();
        if(voucher.getShopName()!= null){
            R<ShopDTO> shop = remoteShopService.getShopByShopName(voucher.getShopName());
            shopId = shop.getData().getId();
        }
        return query().eq(shopId != null,"shop_id", shopId).list();
    }

    /**
     * 查询店铺的秒杀优惠券列表
     *
     * @param voucher
     * @return
     */
    @Override
    public List<Voucher> listSeckillVoucher(Voucher voucher) {
        Long shopId = voucher.getShopId();
        if(voucher.getShopName()!= null){
            R<ShopDTO> shop = remoteShopService.getShopByShopName(voucher.getShopName());
            shopId = shop.getData().getId();
        }
        return query().eq(shopId != null,"shop_id", shopId).eq("type", 1).list();
    }
}
