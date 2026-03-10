package com.smartLive.order.service.impl;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import java.util.ArrayList;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.order.domain.VO.ProductSoldVO;
import com.smartLive.product.api.RemoteProductService;
import com.smartLive.points.api.RemotePointsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.order.domain.VO.OrderVO;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.RemoteShopService;
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
import com.smartLive.common.rabbitmq.domain.StockDeductMessage;
import com.smartLive.common.core.constant.mq.ProductMqConstants;

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
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private RemoteProductService remoteProductService;

    @Autowired
    private RemotePointsService remotePointsService;
    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private RedisService redisService;


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
            ProductDTO productDTO = remoteProductService.getProductById(order.getSourceId());
            if(productDTO!=null && productDTO.getShopId() != null && !productDTO.getShopId().isEmpty()) {
                order.setShopId(Long.valueOf(productDTO.getShopId().split(",")[0]));
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
            ProductDTO product  = remoteProductService.getProductById(v.getSourceId());
            if(product!=null && product.getShopId() != null && !product.getShopId().isEmpty()) {
                v.setShopId(Long.valueOf(product.getShopId().split(",")[0]));
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
            return;
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
        //5.获取商品信息以计算过期时间
        ProductDTO productDTO = remoteProductService.getProductById(order.getSourceId());
        if (productDTO != null && productDTO.getValidityType() != null) {
            if (productDTO.getValidityType() == 1 && productDTO.getUseEndTime() != null) {
                order.setExpireTime(productDTO.getUseEndTime());
            } else if (productDTO.getValidityType() == 2 && productDTO.getValidDays() != null) {
                order.setExpireTime(DateUtils.addDays(DateUtils.getNowDate(), productDTO.getValidDays()));
            }
        }

        // 6.扣减库存（已转为异步批量 MQ 处理，原有的 Feign 同步强拦截已移除）
        // 物理超售防护由商品侧 Listener 和 DB CAS (stock >= count) 承担
        // Redis 原子的超售预拦截保留在此前网关或 Controller 层

        // 7.创建订单
        boolean save = save(order);
        if(!save){
            //创建失败
            log.error("创建订单失败");
            return;
        }else{
            log.info("订单已创建，ID={}，数量={}，发送 MQ 异步扣库指令...", order.getId(), order.getAmount());

            // 构建并发送异步库存扣减消息给商品模块
            StockDeductMessage msg = new StockDeductMessage();
            msg.setProductId(order.getSourceId());
            msg.setOrderId(order.getId());
            msg.setCount(order.getAmount() != null ? order.getAmount() : 1); // 扣减实际购买数量

            mqMessageSendUtils.sendMqMessage(
                ProductMqConstants.PRODUCT_STOCK_EXCHANGE,
                ProductMqConstants.PRODUCT_STOCK_DEDUCT_ROUTING_KEY,
                msg,
                0
            );

            // 创建成功，删除 Redis 占位符
            redisService.deleteObject("order:status:" + order.getId());
            // 发送延迟消息，检测订单支付状态
            mqMessageSendUtils.sendMqMessage( OrderMqConstants.ORDER_DELAY_EXCHANGE,OrderMqConstants.ORDER_DELAY_ROUTING_KEY,order.getId(),(OrderMqConstants.DELAY_TIME));
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
            ProductDTO product  = remoteProductService.getProductById(v.getSourceId());
            if(product!=null) {
                orderVO.setRules(product.getRulesJson()); // Mapped rulesJson to rules
                orderVO.setPayValue(product.getPrice());      // Mapped price to payValue
                orderVO.setActualValue(product.getOriginalPrice()); // Mapped originalPrice to actualValue
                orderVO.setTitle(product.getName());          // Mapped name to title
                orderVO.setSubTitle(product.getSubTitle());
                orderVO.setCoverImg(product.getCoverImg());
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
     * 支付成功更新订单状态（内部调用，支持多种支付方式）
     *
     * @param orderId 订单ID
     * @param payType 支付方式: 1=余额 2=支付宝 3=微信
     * @return 影响行数
     */
    @Override
    public Integer paySuccess(Long orderId, Integer payType) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 幂等校验：已支付则跳过
        if (order.getStatus() == OrderStatusConstants.PAID) {
            return 1;
        }
        order.setPayTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.PAID);
        order.setPayType(payType);
        return updateOrder(order);
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
            ProductDTO vo = remoteProductService.getProductById(order.getSourceId());
            if (vo.getActivityType()==1){ // Mapped activityType (0/1) for logic
                log.info("秒杀商品,准备恢复库存");
                    //恢复库存
                    remoteProductService.recoverStock(order.getSourceId());
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
     * 使用订单 (核销)
     *
     * @param id 订单ID
     * @param shopId 核销的门店ID
     * @return 影响行数
     */
    @Override
    public Integer use(Long id, Long shopId) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("订单不存在");
        }
        order.setUseTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.VERIFIED);
        if (shopId != null) {
            order.setShopId(shopId);
        }
        int i = updateOrder(order);
        if (i > 0) {
            // 订单使用成功，奖励积分
            try {
                remotePointsService.addPoints(order.getUserId(), 100, String.valueOf(order.getId()), "订单完成奖励");
            } catch (Exception e) {
                log.error("订单{}积分奖励失败:{}", order.getId(), e.getMessage());
            }
        }
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
            ProductDTO product  = remoteProductService.getProductById(order.getSourceId());
            if(product!=null) {
                orderVO.setRules(product.getRulesJson());
                orderVO.setPayValue(product.getPrice());
                orderVO.setActualValue(product.getOriginalPrice());
                orderVO.setTitle(product.getName());
                orderVO.setSubTitle(product.getSubTitle());
                orderVO.setCoverImg(product.getCoverImg());
            }
            return orderVO;
        }
        return null;
    }

    /**
     * 修改订单评价状态
     *
     * @param orderId
     * @param reviewId
     * @param reviewTime
     * @return
     */
    @Override
    public Integer updateOrderReviewStatus(Long orderId, Long reviewId, java.util.Date reviewTime) {
        boolean update = update().eq("id", orderId)
                .set("review_status", 1)
                .set("review_id", reviewId)
                .set("review_time", reviewTime)
                .update();
        return update ? 1 : 0;
    }

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Override
    public String getOrderStatus(Long id) {
        // 1. 先查 Redis 里的标识 key (e.g. "order:status:" + id)
        //    假设前端传过来的 id 其实是 snowflake id 或者某种业务 id,
        //    在创建订单前，已经由前端或网关生成并存入 Redis marked as "CREATING"
        String key = "order:status:" + id;
        if (redisService.hasKey(key)) {
            return "PENDING";
        }

        // 2. Redis 没 key 了，说明要么失败要么成功，查数据库
        Order order = getById(id);
        if (order != null) {
            return "SUCCESS";
        }

        // 3. 既没 key 也没库记录 -> 失败
        return "FAILED";
    }

    /**
     * 获取商品销售统计
     *
     * @return
     */
    @Override
    public List<ProductSoldVO> countProductSold() {
        return orderMapper.countProductSold();
    }
}
