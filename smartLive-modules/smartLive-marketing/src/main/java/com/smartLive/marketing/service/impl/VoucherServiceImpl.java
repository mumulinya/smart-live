package com.smartLive.marketing.service.impl;

import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.domain.SeckillVoucher;
import com.smartLive.marketing.service.ISeckillVoucherService;
import com.smartLive.marketing.until.RedisIdWorker;
import com.smartLive.order.api.dto.VoucherOrderDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.smartLive.marketing.mapper.VoucherMapper;
import com.smartLive.marketing.domain.Voucher;
import com.smartLive.marketing.service.IVoucherService;
import org.springframework.transaction.annotation.Transactional;

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
    @Resource
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    private IVoucherService proxy;
    /**
     * 释放锁脚本初始化
     */
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    /**
     * 查询优惠券
     * 
     * @param id 优惠券主键
     * @return 优惠券
     */
    @Override
    public Voucher selectVoucherById(Long id)
    {
        Voucher voucher = voucherMapper.selectVoucherById(id);
        if (voucher != null){
            SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", voucher.getId()).one();
            if(voucher.getType()==1){
                voucher.setStock(seckillVoucher.getStock());
            }
            voucher.setBeginTime(seckillVoucher.getBeginTime());
            voucher.setEndTime(seckillVoucher.getEndTime());
        }
        return voucher;
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
        List<Voucher> voucherList = voucherMapper.selectVoucherList(voucher);
        voucherList.forEach(v -> {
            SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", v.getId()).one();
            if(v.getType()==1){
                v.setStock(seckillVoucher.getStock());
            }
            v.setBeginTime(seckillVoucher.getBeginTime());
            v.setEndTime(seckillVoucher.getEndTime());
        });
        return voucherList;
    }

    /**
     * 新增优惠券
     * 
     * @param voucher 优惠券
     * @return 结果
     */
    @Override
    @Transactional
    public int insertVoucher(Voucher voucher)
    {
        voucher.setCreateTime(DateUtils.getNowDate());
        //添加秒杀券
        if(voucher.getType()==1){
            addSeckillVoucher(voucher);
            return 1;
        }
        //保存优惠券
        int i = voucherMapper.insertVoucher(voucher);
        if(i>0){
            SeckillVoucher seckillVoucher = new SeckillVoucher();
            seckillVoucher.setVoucherId(voucher.getId());
            seckillVoucher.setBeginTime(voucher.getBeginTime());
            seckillVoucher.setEndTime(voucher.getEndTime());
            seckillVoucher.setCreateTime(DateUtils.getNowDate());
            seckillVoucherService.save(seckillVoucher);
        }
        return i;
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
        int i = voucherMapper.updateVoucher(voucher);
        if(i>0){
            SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", voucher.getId()).one();
            if(voucher.getType()==1){
                seckillVoucher.setStock(voucher.getStock());
            }
            seckillVoucher.setBeginTime(voucher.getBeginTime());
            seckillVoucher.setEndTime(voucher.getEndTime());
            seckillVoucher.setUpdateTime(DateUtils.getNowDate());
            seckillVoucherService.updateById(seckillVoucher);
        }
        return i;
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
     * 秒杀优惠券(使用rabbitMq队列创建订单)
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId, Long userId) {
        //获取订单id
        Long orderId = redisIdWorker.nextId("order");
        //1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId));
        int r = result.intValue();
        //2.判断结果是否为0
        if(r != 0){
            //2.1 不为0，代表没有购买资格
            switch (r){
                case 1:
                    return Result.fail("库存不足");
                case 2:
                    return Result.fail("不能重复下单");
            }
        }
        //创建订单

        VoucherOrderDTO voucherOrder = new VoucherOrderDTO();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //发送消息
        rabbitTemplate.convertAndSend(MqConstants.ORDER_EXCHANGE_NAME, MqConstants.ORDER_SECKILL_ROUTING, voucherOrder);
        //获取事务代理对象
        proxy= (IVoucherService) AopContext.currentProxy();
        //3 返回订单id
        return Result.ok(orderId);
    }


    /**
     * 购买优惠券
     * @param voucherId
     * @return
     */
    @Override
    public Result buyVoucher(Long voucherId, Long userId) {
        //获取订单id
        Long orderId = redisIdWorker.nextId("order");
        VoucherOrderDTO voucherOrder = new VoucherOrderDTO();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //5.发送消息创建订单
        //发送消息
        rabbitTemplate.convertAndSend(MqConstants.ORDER_EXCHANGE_NAME, MqConstants.ORDER_BUY_ROUTING, voucherOrder);
//        save(voucherOrder);
        //6.返回订单id
        return Result.ok(voucherOrder.getId());
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
     * @param
     * @return
     */
    @Override
    public List<Voucher> listVoucher( ) {
        List<Voucher> list = query().list();
        list.forEach(voucher -> {
            ShopDTO shopDTO = remoteShopService.getShopById(voucher.getShopId()).getData();
            voucher.setShopName(shopDTO.getName());
            voucher.setTypeId(shopDTO.getTypeId());
        });
        return list;
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
