package com.smartLive.order.service.impl;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.enums.product.ProductStatusEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.order.domain.VO.ProductSoldVO;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import com.smartLive.common.core.constant.mq.PointsMqConstants;
import com.smartLive.common.rabbitmq.domain.OrderPointsMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.enums.product.SalesTypeEnum;
import com.smartLive.order.domain.VO.OrderVO;

import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.system.api.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.smartLive.order.mapper.OrderMapper;
import com.smartLive.order.domain.Order;
import com.smartLive.order.service.IOrderService;
import com.smartLive.common.rabbitmq.domain.StockDeductMessage;
import com.smartLive.common.rabbitmq.domain.OrderRefundMessage;
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
    private RemoteShopService remoteShopService;

    @Autowired
    private RemoteUserService remoteUserService;


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

        return orderMapper.selectOrderById(id);
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
        if (order == null)
        {
            order = new Order();
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && !SecurityUtils.isAdmin(currentUserId))
        {
            List<Long> shopIds = remoteUserService.getShopIdsByUserId(currentUserId);
            if (shopIds == null || shopIds.isEmpty())
            {
                return new ArrayList<>();
            }
            order.setShopIds(shopIds);
            order.setExcludedStatuses(Arrays.asList(OrderStatusConstants.UNPAID, OrderStatusConstants.CANCELLED));
        }

        return orderMapper.selectOrderList(order);
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
        Integer count = query()
                .eq("user_id", userId)
                .eq("source_id", order.getSourceId())
                .notIn("status",
                        OrderStatusConstants.EXPIRED,   // 过期
                        OrderStatusConstants.CANCELLED, // 取消
                        OrderStatusConstants.REFUNDED   // 退款
                ).count().intValue();
        if(count>0){
            //用户已经购买过了
            log.error("用户已经购买过了，触发 Redis 回退");
            // 特殊处理：如果是秒杀场景，虽然数据库挡住了，但 Lua 脚本可能已经扣了预库存，这里尝试回滚
            remoteProductService.recoverRedisStockAndEligibility(order.getSourceId(), null);
            redisService.deleteObject("order:status:" + order.getId());
            return;
        }
        // 7.创建订单
        boolean save = save(order);
        if(!save){
            //创建失败
            log.error("创建订单保存数据库失败，触发 Redis 回退");
            remoteProductService.recoverRedisStockAndEligibility(order.getSourceId(), userId);
            redisService.deleteObject("order:status:" + order.getId());
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
     * 增加商品及其对应店铺的销量
     * 采用“三级回退”策略保证数据初始化：
     * 1. 检查 Redis 计数器；
     * 2. 若 Redis 为空，通过 Feign 调用源模块（Product/Shop）获取已落库数值；
     * 3. 若源模块数值仍不可信或为初始化，则在本模块数据库汇总所有历史订单完成数。
     * 
     * @param order 订单实体
     */
    private void incrementSales(Order order) {
        if (order == null || order.getSourceId() == null) {
            return;
        }

        // 处理商品销量累加
        String productCountKey = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix() + order.getSourceId();
        if (Boolean.FALSE.equals(redisService.hasKey(productCountKey))) {
            initSalesCount(SalesTypeEnum.PRODUCT_SALES, order.getSourceId());
        }
        long amount = order.getAmount() != null ? order.getAmount() : 1;
        redisService.incrementCacheValue(productCountKey, amount);
        redisService.setCacheSet(SalesTypeEnum.PRODUCT_SALES.getDirtyKey(), order.getSourceId().toString());
    }

    /**
     * 增加对应店铺的销量 (仅在订单实际被核销使用时调用)
     */
    private void incrementShopSales(Order order) {
        log.info("订单已核销，开始累加店铺销量...{}", order);
        if (order == null || order.getVerifyShopId() == null) {
            return;
        }
        Long shopId = order.getVerifyShopId();
        String shopCountKey = SalesTypeEnum.SHOP_SALES.getCountKeyPrefix() + shopId;
        if (Boolean.FALSE.equals(redisService.hasKey(shopCountKey))) {
            initSalesCount(SalesTypeEnum.SHOP_SALES, shopId);
        }
        long amount = order.getAmount() != null ? order.getAmount() : 1;
        redisService.incrementCacheValue(shopCountKey, amount);
        redisService.setCacheSet(SalesTypeEnum.SHOP_SALES.getDirtyKey(), shopId.toString());
    }

    /**
     * 回退商品及店铺销量 (订单取消或退款时调用)
     *
     * @param order 订单实体
     * @param decrementShop 是否需要同时回退店铺销量
     */
    private void decrementSales(Order order, boolean decrementShop) {
        if (order == null || order.getSourceId() == null) {
            return;
        }
        long amount = order.getAmount() != null ? order.getAmount() : 1;

        // 1. 回退商品销量
        String productCountKey = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix() + order.getSourceId();
        if (Boolean.TRUE.equals(redisService.hasKey(productCountKey))) {
            redisService.decrementCacheValue(productCountKey, amount);
            redisService.setCacheSet(SalesTypeEnum.PRODUCT_SALES.getDirtyKey(), order.getSourceId().toString());
        }

        // 2. 回退店铺销量 (仅当订单已被核销，且有对应 shopId 时)
        if (decrementShop && order.getVerifyShopId() != null) {
            Long shopId = order.getVerifyShopId();
            String shopCountKey = SalesTypeEnum.SHOP_SALES.getCountKeyPrefix() + shopId;
            if (Boolean.TRUE.equals(redisService.hasKey(shopCountKey))) {
                redisService.decrementCacheValue(shopCountKey, amount);
                redisService.setCacheSet(SalesTypeEnum.SHOP_SALES.getDirtyKey(), shopId.toString());
            }
        }
    }

    /**
     * 初始化销量计数器（三级策略）
     */
    private void initSalesCount(SalesTypeEnum type, Long id) {
        Integer baseline = 0;
        Integer totalOrders = 0;
        String countKey = type.getCountKeyPrefix() + id;

        if (type == SalesTypeEnum.PRODUCT_SALES) {
            // 从商品模块获取已持久化的销量基数
            baseline = remoteProductService.getSold(id);
            // 从订单库获取全部已确认/支付的订单总量（兜底全量初始化）
            totalOrders = orderMapper.sumSoldBySourceId(id);
        } else if (type == SalesTypeEnum.SHOP_SALES) {
            // 从店铺模块获取已持久化的销量基数
            baseline = remoteShopService.getSold(id);
            // 从订单库获取该店铺全部销量
            totalOrders = orderMapper.sumSoldByShopId(id);
        }

        // 逻辑：如果基数已经包含了订单库的历史数据，则直接用基数；
        // 初始化时应确保 Redis 中是当前最准确的【全量总数】。
        // 根据要求：“先去源模块获取数据，然后再去本模块查询”，这里取两者之和。
        int initialCount = (baseline != null ? baseline : 0) + (totalOrders != null ? totalOrders : 0);
        redisService.setCacheObject(countKey, initialCount);
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
        updateExpireTimeAfterPayment(order);
        // 支付成功，累加商品销量统计
        incrementSales(order);
        return updateOrder(order);
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
        updateExpireTimeAfterPayment(order);
        // 支付成功，累加商品销量统计
        incrementSales(order);
        return updateOrder(order);
    }

    /**
     * 支付成功后（或付款瞬间），为特定商品类型计算真正的有效期截止时间。
     * 避免因支付倒计时（如15分钟）导致用户亏损使用期。
     *
     * @param order 订单
     */
    private void updateExpireTimeAfterPayment(Order order) {
        if (order == null || order.getSourceId() == null) {
            return;
        }
        ProductDTO productDTO = remoteProductService.getProductById(order.getSourceId());
        if (productDTO != null && productDTO.getValidityType() != null) {
            Date now = DateUtils.getNowDate();
            // “购买后 N 天内有效”：支付成功这一刻起算
            if (productDTO.getValidityType() == 2 && productDTO.getValidDays() != null) {
                order.setValidStartTime(now);
                order.setExpireTime(DateUtils.addDays(now, productDTO.getValidDays()));
            }
            // 固定有效期
            else if (productDTO.getValidityType() == 1) {
                if (productDTO.getUseStartTime() != null) {
                    order.setValidStartTime(productDTO.getUseStartTime());
                }
                if (productDTO.getUseEndTime() != null) {
                    order.setExpireTime(productDTO.getUseEndTime());
                }
            }
        }
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
        Integer oldStatus = order.getStatus();
        order.setStatus(OrderStatusConstants.CANCELLED);
        int i = updateOrder(order);
        if(i>0){
            ProductDTO vo = remoteProductService.getProductById(order.getSourceId());
            if (vo != null && vo.getActivityType() != null && vo.getActivityType() == 1&&vo.getStatus().equals(ProductStatusEnum.ON_SHELF.getCode())){
                log.info("秒杀商品取消,准备恢复库存");
                // 恢复库存
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            // 订单取消。如果是已支付订单取消（例如管理员操作），则回退商品销量 (未核销过，不需要回退店铺销量)
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                decrementSales(order, false);
                // 发送MQ消息通知钱包模块，将退款金额退回到用户余额
                sendRefundMessage(order);
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
        Integer oldStatus = order.getStatus();
        order.setRefundTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.REFUNDED);
        int i = updateOrder(order);
        if(i>0){
            ProductDTO vo = remoteProductService.getProductById(order.getSourceId());
            if (vo != null && vo.getActivityType() != null && vo.getActivityType() == 1&&vo.getStatus().equals(ProductStatusEnum.ON_SHELF.getCode())){
                log.info("秒杀商品退款,准备恢复库存");
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            // 订单退款。如果是已支付订单（status >= PAID）退款，回滚商品销量。若处于已核销状态退款，同时回退店铺销量
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                boolean wasVerified = (oldStatus == OrderStatusConstants.VERIFIED);
                decrementSales(order, wasVerified);
                // 发送MQ消息通知钱包模块，将退款金额退回到用户余额
                sendRefundMessage(order);
            }
        }
        return i;
    }

    /**
     * 发送退款消息到钱包模块（所有支付方式均退款至余额）
     *
     * @param order 订单
     */
    private void sendRefundMessage(Order order) {
        OrderRefundMessage msg = new OrderRefundMessage();
        msg.setOrderId(order.getId());
        msg.setUserId(order.getUserId());
        msg.setAmount(order.getPayAmount());
        msg.setPayType(order.getPayType());
        mqMessageSendUtils.sendMqMessage(
                OrderMqConstants.ORDER_REFUND_EXCHANGE,
                OrderMqConstants.ORDER_REFUND_ROUTING_KEY,
                msg
        );
        log.info("已发送退款MQ消息, orderId={}, userId={}, amount={}", order.getId(), order.getUserId(), order.getPayAmount());
    }

    /**
     * 使用订单 (核销)
     *
     * @param id 订单ID
     * @param verifyShopId 核销的门店ID
     * @return 影响行数
     */
    @Override
    public Integer use(Long id, Long verifyShopId) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("订单不存在");
        }
        if (verifyShopId == null) {
            throw new BusinessException("核销门店不能为空");
        }
        String availableShopIds = order.getShopId();
        if (availableShopIds != null && !availableShopIds.isEmpty()) {
            boolean matched = false;
            for (String shopIdItem : availableShopIds.split(",")) {
                if (verifyShopId.toString().equals(shopIdItem.trim())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new BusinessException("核销门店不在可用门店范围内");
            }
        }
        order.setUseTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.VERIFIED);
        order.setVerifyShopId(verifyShopId);
        int i = updateOrder(order);
        if (i > 0) {
            // 核销成功，增加门店销量
            incrementShopSales(order);
            // 订单核销成功，发送MQ消息异步奖励积分（积分 = 实付金额）
            if (order.getPayAmount() != null && order.getPayAmount().intValue() > 0) {
                OrderPointsMessage pointsMsg = new OrderPointsMessage();
                pointsMsg.setOrderId(order.getId());
                pointsMsg.setUserId(order.getUserId());
                pointsMsg.setPayAmount(order.getPayAmount());
                mqMessageSendUtils.sendMqMessage(
                        PointsMqConstants.POINTS_DIRECT_EXCHANGE,
                        PointsMqConstants.POINTS_ORDER_ROUTING_KEY,
                        pointsMsg
                );
                log.info("已发送积分奖励MQ消息, orderId={}, userId={}, payAmount={}", order.getId(), order.getUserId(), order.getPayAmount());
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

    @Override
    public Integer countWeekOrders(Long shopId) {
        if (shopId == null) {
            return 0;
        }
        Integer count = orderMapper.countWeekOrders(shopId);
        return count == null ? 0 : count;
    }

    /**
     * 订单过期
     *
     * @param id
     * @return
     */
    @Override
    public Integer expired(Long id) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("订单不存在");
        }
        Integer oldStatus = order.getStatus();
        order.setStatus(OrderStatusConstants.EXPIRED);
        int i = updateOrder(order);
        if(i>0){
            ProductDTO vo = remoteProductService.getProductById(order.getSourceId());
            if (vo != null && vo.getActivityType() != null && vo.getActivityType() == 1&&vo.getStatus().equals(ProductStatusEnum.ON_SHELF.getCode())){
                log.info("秒杀商品订单过期,准备恢复库存");
                // 恢复库存
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            // 订单取消。如果是已支付订单取消（例如管理员操作），则回退商品销量 (未核销过，不需要回退店铺销量)
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                decrementSales(order, false);
                // 发送MQ消息通知钱包模块，将退款金额退回到用户余额
                sendRefundMessage(order);
            }
        }
        return i;
    }
}
