package com.smartLive.order.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.marketing.api.RemoteMarketingService;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.api.dto.VoucherDTO;
import com.smartLive.order.until.RedisIdWorker;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.smartLive.order.mapper.VoucherOrderMapper;
import com.smartLive.order.domain.VoucherOrder;
import com.smartLive.order.service.IVoucherOrderService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 优惠券订单表Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService
{
    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 查询优惠券订单表
     * 
     * @param id 优惠券订单表主键
     * @return 优惠券订单表
     */
    @Override
    public VoucherOrder selectVoucherOrderById(Long id)
    {

        VoucherOrder voucherOrder = voucherOrderMapper.selectVoucherOrderById(id);
        if (voucherOrder!=null) {
            VoucherDTO voucherDTO = (VoucherDTO) remoteMarketingService.getVoucherById(voucherOrder.getVoucherId()).getData();
            voucherOrder.setShopId(voucherDTO.getShopId());
        }
        return voucherOrder;
    }

    /**
     * 查询优惠券订单表列表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 优惠券订单表
     */
    @Override
    public List<VoucherOrder> selectVoucherOrderList(VoucherOrder voucherOrder)
    {
        List<VoucherOrder> voucherOrderList = voucherOrderMapper.selectVoucherOrderList(voucherOrder);
        voucherOrderList.forEach(v -> {
            Object data = remoteMarketingService.getVoucherById(v.getVoucherId()).getData();
            // 使用 ObjectMapper 将 LinkedHashMap 转换为 VoucherDTO
            ObjectMapper objectMapper = new ObjectMapper();
            VoucherDTO voucherDTO = objectMapper.convertValue(data, VoucherDTO.class);
            v.setShopId(voucherDTO.getShopId());
        });
        return voucherOrderList;
    }

    /**
     * 新增优惠券订单表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 结果
     */
    @Override
    public int insertVoucherOrder(VoucherOrder voucherOrder)
    {
        voucherOrder.setCreateTime(DateUtils.getNowDate());
        return voucherOrderMapper.insertVoucherOrder(voucherOrder);
    }

    /**
     * 修改优惠券订单表
     * 
     * @param voucherOrder 优惠券订单表
     * @return 结果
     */
    @Override
    public int updateVoucherOrder(VoucherOrder voucherOrder)
    {
        voucherOrder.setUpdateTime(DateUtils.getNowDate());
        return voucherOrderMapper.updateVoucherOrder(voucherOrder);
    }

    /**
     * 批量删除优惠券订单表
     * 
     * @param ids 需要删除的优惠券订单表主键
     * @return 结果
     */
    @Override
    public int deleteVoucherOrderByIds(Long[] ids)
    {
        return voucherOrderMapper.deleteVoucherOrderByIds(ids);
    }

    /**
     * 删除优惠券订单表信息
     * 
     * @param id 优惠券订单表主键
     * @return 结果
     */
    @Override
    public int deleteVoucherOrderById(Long id)
    {
        return voucherOrderMapper.deleteVoucherOrderById(id);
    }


    @Autowired
    private RemoteMarketingService remoteMarketingService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    /**
     * 释放锁脚本初始化
     */
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    private IVoucherOrderService proxy;

    /**
     * 处理订单
     * @param voucherOrder
     */

    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        //获取事务代理对象
        proxy= (IVoucherOrderService) AopContext.currentProxy();

        //1。获取用户id
        Long userId = voucherOrder.getUserId();
        //2.获取redisson锁对象
        RLock lock = redissonClient.getLock("order:" + userId);
        //3.获取锁
        boolean isLock = lock.tryLock();
        //判断锁是否获取成功
        if(!isLock){
            //获取锁失败,返回错误信息
            log.error("不允许重复下单");
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //释放锁
            lock.unlock();
        }

    }

    /**
     *实现一人一单
     * @param voucherOrder
     * @return
     */
    public  void createVoucherOrder(VoucherOrder voucherOrder) {
        //获取当前用户id
        Long userId = voucherOrder.getUserId();
        //判断当前用户是否购买过
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count().intValue();
        if(count>0){
            //用户已经购买过了
            log.error("用户已经购买过了");
        }
        //5.扣减库存
        R<Boolean> r = remoteMarketingService.updateVoucherById(voucherOrder.getVoucherId());
        boolean success = r.getData();
        if(!success){
            //扣减失败
            log.error("库存不足");
        }
        //6.创建订单
        save(voucherOrder);
    }

    /**
     * 获取当前用户订单列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<VoucherOrder> queryMyVoucherOrderList(Long userId,Integer current) {
        Page<VoucherOrder> result = query()
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<VoucherOrder> list = result.getRecords();
        return list;
    }

    /**
     * 支付订单
     *
     * @param id
     * @param userId
     * @return
     */
    @Override
    public Result pay(Long id, Long userId) {
        VoucherOrder voucherOrder = query().eq("id", id).eq("user_id", userId).one();
        if(voucherOrder==null){
            return Result.fail("订单不存在");
        }
        voucherOrder.setPayTime(DateUtils.getNowDate());
        voucherOrder.setStatus(OrderStatusConstants.PAID);
        voucherOrder.setPayType(PayTypeConstants.BALANCE);
        int i = updateVoucherOrder(voucherOrder);
        if(i>0){
            return Result.ok("付款成功");
        }
        return Result.fail("付款失败");
    }

    /**
     * 取消订单
     *
     * @param id
     * @param userId
     * @return
     */
    @Override
    public Result cancel(Long id, Long userId) {
        VoucherOrder voucherOrder = query().eq("id", id).eq("user_id", userId).one();
        if(voucherOrder==null){
            return Result.fail("订单不存在");
        }
        voucherOrder.setStatus(OrderStatusConstants.CANCELLED);
        int i = updateVoucherOrder(voucherOrder);
        if(i>0){
            return Result.ok("已经取消");
        }
        return Result.fail("取消失败");
    }

    /**
     * 退款订单
     *
     * @param id
     * @param userId
     * @return
     */
    @Override
    public Result refund(Long id, Long userId) {
        VoucherOrder voucherOrder = query().eq("id", id).eq("user_id", userId).one();
        if(voucherOrder==null){
            return Result.fail("订单不存在");
        }
        voucherOrder.setRefundTime(DateUtils.getNowDate());
        voucherOrder.setStatus(OrderStatusConstants.REFUNDED);
        int i = updateVoucherOrder(voucherOrder);
        if(i>0){
            return Result.ok("退款成功");
        }
        return Result.fail("退款失败");
    }

    /**
     * 使用订单
     *
     * @param id
     * @param userId
     * @return
     */
    @Override
    public Result use(Long id, Long userId) {
        VoucherOrder voucherOrder = query().eq("id", id).eq("user_id", userId).one();
        if(voucherOrder==null){
            return Result.fail("订单不存在");
        }
        voucherOrder.setUseTime(DateUtils.getNowDate());
        voucherOrder.setStatus(OrderStatusConstants.VERIFIED);
        int i = updateVoucherOrder(voucherOrder);
        if(i>0){
            return Result.ok("使用成功");
        }
        return Result.fail("使用失败");
    }

    /**
     * 获取订单数量
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getOrderCount(Long userId) {
        int orderCount = query().eq("user_id", userId).count().intValue();
        System.out.println("订单数量为:"+orderCount);
        return orderCount;
    }
}
