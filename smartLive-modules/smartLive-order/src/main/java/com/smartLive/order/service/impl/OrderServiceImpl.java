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
import com.smartLive.order.domain.VO.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.smartLive.order.mapper.OrderMapper;
import com.smartLive.order.domain.Order;
import com.smartLive.order.service.IOrderService;

import jakarta.annotation.Resource;

/**
 * 订单表Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService
{
    @Autowired
    private OrderMapper orderMapper;

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
    private IOrderService proxy;

    /**
     * 查询订单表
     * 
     * @param id 订单表主键
     * @return 订单表
     */
    @Override
    public Order selectOrderById(Long id)
    {

        Order order = orderMapper.selectOrderById(id);
        if (order!=null) {
            VoucherDTO voucherDTO = remoteVoucherService.getVoucherById(order.getSourceId());
            if(voucherDTO!=null) {
                order.setShopId(voucherDTO.getShopId());
            }
        }
        return order;
    }

    /**
     * 查询订单表列表
     * 
     * @param order 订单表
     * @return 订单表
     */
    @Override
    public List<Order> selectOrderList(Order order)
    {
        List<Order> orderList = orderMapper.selectOrderList(order);
        orderList.forEach(v -> {
            VoucherDTO voucher  = remoteVoucherService.getVoucherById(v.getSourceId());
            if(voucher!=null) {
                v.setShopId(voucher.getShopId());
            }
        });
        return orderList;
    }

    /**
     * 新增订单表
     * 
     * @param order 订单表
     * @return 结果
     */
    @Override
    public int insertOrder(Order order)
    {
        order.setCreateTime(DateUtils.getNowDate());
        return orderMapper.insertOrder(order);
    }

    /**
     * 修改订单表
     * 
     * @param order 订单表
     * @return 结果
     */
    @Override
    public int updateOrder(Order order)
    {
        order.setUpdateTime(DateUtils.getNowDate());
        return orderMapper.updateOrder(order);
    }

    /**
     * 批量删除订单表
     * 
     * @param ids 需要删除的订单表主键
     * @return 结果
     */
    @Override
    public int deleteOrderByIds(Long[] ids)
    {
        return orderMapper.deleteOrderByIds(ids);
    }

    /**
     * 删除订单表信息
     * 
     * @param id 订单表主键
     * @return 结果
     */
    @Override
    public int deleteOrderById(Long id)
    {
        return orderMapper.deleteOrderById(id);
    }

    /**
     * 处理订单
     * @param order
     */

    public void handleOrder(Order order) {
        //获取事务代理对象
        proxy= (IOrderService) AopContext.currentProxy();

        //1。获取用户id
        Long userId = order.getUserId();
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
            proxy.createOrder(order);
        }catch (Exception e){
            log.error("创建订单失败",e);
        }finally {
            //释放锁
            lock.unlock();
        }

    }

    /**
     *实现一人一单
     * @param order
     * @return
     */
    public  void createOrder(Order order) {
        //获取当前用户id
        Long userId = order.getUserId();
        //判断当前用户是否购买过
        Integer count = query().eq("user_id", userId).eq("source_id", order.getSourceId()).count().intValue();
        if(count>0){
            //用户已经购买过了
            log.error("用户已经购买过了");
            return;
        }
        //5.扣减库存
        Boolean success = remoteVoucherService.updateVoucherById(order.getSourceId());
        if(!success){
            //扣减失败
            log.error("库存不足");
            return;
        }
        //6.创建订单
        boolean save = save(order);
        if(!save){
            //创建失败
            log.error("创建订单失败");
            return;
        }else{
            //发送延迟消息，检测订单支付状态
            MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ORDER_DELAY_EXCHANGE_NAME,MqConstants.ORDER_DELAY_ROUTING,order.getId(),(MqConstants.DELAY_TIME));
        }
    }

    /**
     * 获取当前用户订单列表
     *
     * @param order
     * @return
     */
    @Override
    public List<OrderVO> queryMyOrderList(Order order, Integer current) {
        Page<Order> result = query()
                .eq("user_id", order.getUserId())
                .eq(order.getStatus()!=null, "status", order.getStatus())
                .eq(order.getReviewStatus()!=null, "review_status", order.getReviewStatus())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<Order> list = result.getRecords();
        List<OrderVO> orderVOList=new ArrayList<>();
        list.forEach(v -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(v, orderVO);;
            VoucherDTO voucher  = remoteVoucherService.getVoucherById(v.getSourceId());
            if(voucher!=null) {
                orderVO.setShopId(voucher.getShopId());
                orderVO.setShopName(voucher.getShopName());
                orderVO.setRules(voucher.getRules());
                orderVO.setPayValue(voucher.getPayValue());
                orderVO.setActualValue(voucher.getActualValue());
                orderVO.setTitle(voucher.getTitle());
                orderVO.setSubTitle(voucher.getSubTitle());
            }
            orderVOList.add(orderVO);
        });
        return orderVOList;
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
        Order order = getById(id);
        if(order==null){

            throw new BusinessException("订单不存在");
        }
        order.setPayTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.PAID);
        order.setPayType(PayTypeConstants.BALANCE);
        int i = updateOrder(order);
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
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatusConstants.CANCELLED);
        int i = updateOrder(order);
        if(i>0){
            VoucherDTO vo = remoteVoucherService.getVoucherById(order.getSourceId());
            if (vo.getType()==1){
                log.info("秒杀券,准备恢复库存");
                    //秒杀券
                    //恢复库存
                    remoteVoucherService.recoverVoucherStock(order.getSourceId());
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
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("订单不存在");
        }
        order.setRefundTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.REFUNDED);
        int i = updateOrder(order);
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
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("订单不存在");
        }
        order.setUseTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.VERIFIED);
        int i = updateOrder(order);
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
    public OrderVO getOrderById(Long id) {
        Order order = selectOrderById(id);
        if (order != null) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);;
            VoucherDTO voucher  = remoteVoucherService.getVoucherById(order.getSourceId());
            if(voucher!=null) {
                orderVO.setShopId(voucher.getShopId());
                orderVO.setShopName(voucher.getShopName());
                orderVO.setRules(voucher.getRules());
                orderVO.setPayValue(voucher.getPayValue());
                orderVO.setActualValue(voucher.getActualValue());
                orderVO.setTitle(voucher.getTitle());
                orderVO.setSubTitle(voucher.getSubTitle());
            }
            return orderVO;
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