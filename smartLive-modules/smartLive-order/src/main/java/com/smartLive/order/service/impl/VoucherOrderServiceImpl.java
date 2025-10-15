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
import com.smartLive.order.until.RedisIdWorker;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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

    /**
     * 查询优惠券订单表
     * 
     * @param id 优惠券订单表主键
     * @return 优惠券订单表
     */
    @Override
    public VoucherOrder selectVoucherOrderById(Long id)
    {
        return voucherOrderMapper.selectVoucherOrderById(id);
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
        return voucherOrderMapper.selectVoucherOrderList(voucherOrder);
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
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    private IVoucherOrderService proxy;

    /**
     * 初始化
     */
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    String queueName = "stream.orders";
    private class VoucherOrderHandler implements Runnable {
        /**
         * 1.获取队列中的订单信息
         * 2.创建订单
         * 3.返回结果
         */
        @Override
        public void run() {
            while (true){
                try {
                    //1.获取消息队列中的订单信息 XREADGROUP GROUP  g1 c1 COUNT 1 BLOCK 1000 STREAMS streams.order >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(1000)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2.判断消息是否获取成功
                    if (list == null || list.isEmpty()){
                        //获取失败，说明没有消息，进行下一次循环
                        continue;
                    }
                    //解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //2.2.如果获取成功，可以订单
                    handleVoucherOrder(voucherOrder);
                    //4.ACK确认 消息处理成功 SACK streams.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                    //5.处理异常 订单消息体
                    handlePendingList();
                }
            }
        }
    }

    /**
     * 处理pending-list中的订单
     */
    private void handlePendingList() {
        while (true){
            try {
                //1.获取pending-list中的订单信息 XREADGROUP GROUP  g1 c1 COUNT 1 BLOCK 1000 STREAMS streams.order 0
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(queueName, ReadOffset.from("0"))
                );
                //2.判断消息是否获取成功
                if (list == null || list.isEmpty()){
                    //获取失败，说明pending-list没有消息，退出循环
                    break;
                }
                MapRecord<String, Object, Object> record = list.get(0);
                //2.解析消息中的订单信息
                Map<Object, Object> value = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                //2.2.如果获取成功，可以订单
                handleVoucherOrder(voucherOrder);
                //4.ACK确认 消息处理成功 SACK streams.orders g1 id
                stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
            } catch (Exception e) {
                log.error("处理pending-list订单异常",e);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
    }

    /**
     * 秒杀优惠券(优化版，使用redis的Stream队列)
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

        //获取事务代理对象
        proxy= (IVoucherOrderService) AopContext.currentProxy();
        //3 返回订单id
        return Result.ok(orderId);
    }


    /**
     * 处理订单
     * @param voucherOrder
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
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
     * 购买优惠券
     * @param voucherId
     * @return
     */
    @Override
    public Result buyVoucher(Long voucherId, Long userId) {
        //2.判断当前用户是否购买过
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count().intValue();
        if(count>0){
            //用户已经购买过了
            log.error("用户已经购买过了");
            return Result.fail("用户已经购买过了");
        }
        //4.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //5.保存订单
        save(voucherOrder);
        //6.返回订单id
        return Result.ok(voucherOrder.getId());
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
