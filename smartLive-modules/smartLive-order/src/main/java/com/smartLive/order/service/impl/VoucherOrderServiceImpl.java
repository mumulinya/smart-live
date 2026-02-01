package com.smartLive.order.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.marketing.api.DTO.VoucherDTO;
import com.smartLive.order.domain.VO.VoucherOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.smartLive.order.mapper.VoucherOrderMapper;
import com.smartLive.order.domain.VoucherOrder;
import com.smartLive.order.service.IVoucherOrderService;

import jakarta.annotation.Resource;

/**
 * 优惠券订单表Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService
{
    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RemoteVoucherService remoteVoucherService;

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
            VoucherDTO voucherDTO = remoteVoucherService.getVoucherById(voucherOrder.getVoucherId());
            if(voucherDTO!=null) {
                voucherOrder.setShopId(voucherDTO.getShopId());
            }
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
            VoucherDTO voucher  = remoteVoucherService.getVoucherById(v.getVoucherId());
            if(voucher!=null) {
                v.setShopId(voucher.getShopId());
            }
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
            log.error("创建订单失败",e);
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
            return;
        }
        //5.扣减库存
        Boolean success = remoteVoucherService.updateVoucherById(voucherOrder.getVoucherId());
        if(!success){
            //扣减失败
            log.error("库存不足");
            return;
        }
        //6.创建订单
        boolean save = save(voucherOrder);
        if(!save){
            //创建失败
            log.error("创建订单失败");
            return;
        }else{
            //发送延迟消息，检测订单支付状态
            MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ORDER_DELAY_EXCHANGE_NAME,MqConstants.ORDER_DELAY_ROUTING,voucherOrder.getId(),(MqConstants.DELAY_TIME));
        }
    }

    /**
     * 获取当前用户订单列表
     *
     * @param voucherOrder
     * @return
     */
    @Override
    public List<VoucherOrderVO> queryMyVoucherOrderList(VoucherOrder voucherOrder, Integer current) {
        Page<VoucherOrder> result = query()
                .eq("user_id", voucherOrder.getUserId())
                .eq(voucherOrder.getStatus()!=null, "status", voucherOrder.getStatus())
                .eq(voucherOrder.getReviewStatus()!=null, "review_status", voucherOrder.getReviewStatus())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<VoucherOrder> list = result.getRecords();
        List<VoucherOrderVO> voucherOrderVOList=new ArrayList<>();
        list.forEach(v -> {
            VoucherOrderVO voucherOrderVO = new VoucherOrderVO();
            BeanUtils.copyProperties(v, voucherOrderVO);;
            VoucherDTO voucher  = remoteVoucherService.getVoucherById(v.getVoucherId());
            if(voucher!=null) {
                voucherOrderVO.setShopId(voucher.getShopId());
                voucherOrderVO.setShopName(voucher.getShopName());
                voucherOrderVO.setRules(voucher.getRules());
                voucherOrderVO.setPayValue(voucher.getPayValue());
                voucherOrderVO.setActualValue(voucher.getActualValue());
                voucherOrderVO.setTitle(voucher.getTitle());
                voucherOrderVO.setSubTitle(voucher.getSubTitle());
            }
            voucherOrderVOList.add(voucherOrderVO);
        });
        return voucherOrderVOList;
    }

    /**
     * 支付订单
     *
     * @param id
     * @param
     * @return
     */
    @Override
    public Integer pay(Long id) {
        VoucherOrder voucherOrder = getById(id);
        if(voucherOrder==null){

            throw new BusinessException("订单不存在");
        }
        voucherOrder.setPayTime(DateUtils.getNowDate());
        voucherOrder.setStatus(OrderStatusConstants.PAID);
        voucherOrder.setPayType(PayTypeConstants.BALANCE);
        int i = updateVoucherOrder(voucherOrder);
        return i;
    }

    /**
     * 取消订单
     *
     * @param id
     * @param
     * @return
     */
    @Override
    public Integer cancel(Long id) {
        VoucherOrder voucherOrder = getById(id);
        if(voucherOrder==null){
            throw new BusinessException("订单不存在");
        }
        voucherOrder.setStatus(OrderStatusConstants.CANCELLED);
        int i = updateVoucherOrder(voucherOrder);
        if(i>0){
            VoucherDTO vo = remoteVoucherService.getVoucherById(voucherOrder.getVoucherId());
            if (vo.getType()==1){
                log.info("秒杀券,准备恢复库存");
                    //秒杀券
                    //恢复库存
                    remoteVoucherService.recoverVoucherStock(voucherOrder.getVoucherId());
            }
        }
        return i;
    }

    /**
     * 退款订单
     *
     * @param id
     * @param
     * @return
     */
    @Override
    public Integer refund(Long id) {
        VoucherOrder voucherOrder = getById(id);
        if(voucherOrder==null){
            throw new BusinessException("订单不存在");
        }
        voucherOrder.setRefundTime(DateUtils.getNowDate());
        voucherOrder.setStatus(OrderStatusConstants.REFUNDED);
        int i = updateVoucherOrder(voucherOrder);
        return i;
    }

    /**
     * 使用订单
     *
     * @param id
     * @param
     * @return
     */
    @Override
    public Integer use(Long id) {
        VoucherOrder voucherOrder = getById(id);
        if(voucherOrder==null){
            throw new BusinessException("订单不存在");
        }
        voucherOrder.setUseTime(DateUtils.getNowDate());
        voucherOrder.setStatus(OrderStatusConstants.VERIFIED);
        int i = updateVoucherOrder(voucherOrder);
        return i;
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

    /**
     * 获取订单总数
     *
     * @return
     */
    @Override
    public Integer getOrderTotal() {
        return query().count().intValue();
    }
    /**
     * 根据id查询订单
     *
     * @param id
     * @return
     */
    @Override
    public VoucherOrderVO getOrderById(Long id) {
        VoucherOrder voucherOrder = selectVoucherOrderById(id);
        if (voucherOrder != null) {
            VoucherOrderVO voucherOrderVO = new VoucherOrderVO();
            BeanUtils.copyProperties(voucherOrder, voucherOrderVO);;
            VoucherDTO voucher  = remoteVoucherService.getVoucherById(voucherOrder.getVoucherId());
            if(voucher!=null) {
                voucherOrderVO.setShopId(voucher.getShopId());
                voucherOrderVO.setShopName(voucher.getShopName());
                voucherOrderVO.setRules(voucher.getRules());
                voucherOrderVO.setPayValue(voucher.getPayValue());
                voucherOrderVO.setActualValue(voucher.getActualValue());
                voucherOrderVO.setTitle(voucher.getTitle());
                voucherOrderVO.setSubTitle(voucher.getSubTitle());
            }
            return voucherOrderVO;
        }
        return null;
    }

    /**
     * 修改订单评价状态
     *
     * @param orderId
     * @return
     */
    @Override
    public Integer updateOrderReviewStatus(Long orderId) {
        boolean update = update().eq("id", orderId).set("review_status", 1).update();
        return update==true?1:0;
    }
}
