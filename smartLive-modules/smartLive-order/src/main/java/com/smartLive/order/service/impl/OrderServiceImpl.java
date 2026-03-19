package com.smartLive.order.service.impl;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
import com.smartLive.order.domain.VO.ProductSalesVO;
import com.smartLive.order.domain.VO.ProductSoldVO;
import com.smartLive.order.domain.VO.ShopOrderAnalysisVO;
import com.smartLive.order.domain.VO.ShopOrderSuggestVO;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import com.smartLive.common.core.constant.mq.PointsMqConstants;
import com.smartLive.common.rabbitmq.domain.OrderPointsMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.enums.product.SalesTypeEnum;
import com.smartLive.order.domain.VO.OrderVO;

import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.system.api.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.smartLive.order.mapper.OrderMapper;
import com.smartLive.order.domain.Order;
import com.smartLive.order.service.IOrderService;
import com.smartLive.common.rabbitmq.domain.StockDeductMessage;
import com.smartLive.common.rabbitmq.domain.OrderRefundMessage;
import com.smartLive.common.core.constant.mq.ProductMqConstants;

import jakarta.annotation.Resource;

/**
 * 订单服务实现类，负责订单创建、支付、核销及统计等业务处理。
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService
{
    private static final DateTimeFormatter BUSINESS_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<Integer> BUSINESS_ORDER_STATUSES = List.of(OrderStatusConstants.PAID, OrderStatusConstants.VERIFIED);
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

    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private RedisService redisService;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    private IOrderService proxy;

    /**
     * 根据订单ID查询订单。
     *
     * @param id 订单ID
     * @return 订单实体
     */
    @Override
    public Order selectOrderById(Long id)
    {

        return orderMapper.selectOrderById(id);
    }

    /**
     * 查询订单列表并补充商品、店铺与用户信息。
     *
     * @param order 查询条件
     * @return 订单列表
     */
    @Override
    public List<OrderVO> selectOrderList(Order order)
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
        // 获取订单列表
        List<Order> orderList = orderMapper.selectOrderList(order);
        if (orderList == null || orderList.isEmpty())
        {
            return new ArrayList<>();
        }
        List<OrderVO> orderVOList = new ArrayList<>(orderList.size());
        orderList.forEach(item -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(item, orderVO);
            orderVOList.add(orderVO);
        });
        fillOrderProductNames(orderVOList);
        fillOrderShopNames(orderVOList);
        fillOrderUserNames(orderVOList);
        return orderVOList;
    }

    /**
     * 新增订单并写入创建时间。
     *
     * @param order 订单信息
     * @return 影响行数
     */
    @Override
    public int insertOrder(Order order)
    {
        order.setCreateTime(DateUtils.getNowDate());
        return orderMapper.insertOrder(order);
    }

    /**
     * 更新订单并写入更新时间。
     *
     * @param order 订单信息
     * @return 影响行数
     */
    @Override
    public int updateOrder(Order order)
    {
        order.setUpdateTime(DateUtils.getNowDate());
        return orderMapper.updateOrder(order);
    }

    /**
     * 批量删除订单。
     *
     * @param ids 订单ID数组
     * @return 影响行数
     */
    @Override
    public int deleteOrderByIds(Long[] ids)
    {
        return orderMapper.deleteOrderByIds(ids);
    }

    /**
     * 删除单个订单。
     *
     * @param id 订单ID
     * @return 影响行数
     */
    @Override
    public int deleteOrderById(Long id)
    {
        return orderMapper.deleteOrderById(id);
    }

    /**
     * 创建订单并触发库存扣减与延迟检查消息。
     *
     * @param order 订单信息
     */
    public  void createOrder(Order order) {
        Long userId = order.getUserId();
        Integer count = query()
                .eq("user_id", userId)
                .eq("source_id", order.getSourceId())
                .notIn("status",
                        OrderStatusConstants.EXPIRED,   // 排除已过期订单，避免重复创建
                        OrderStatusConstants.CANCELLED,
                        OrderStatusConstants.REFUNDED
                ).count().intValue();
        if(count>0){
            log.error("duplicate order detected, recover redis stock and eligibility");
            remoteProductService.recoverRedisStockAndEligibility(order.getSourceId(), null);
            redisService.deleteObject("order:status:" + order.getId());
            return;
        }
        boolean save = save(order);
        if(!save){
            log.error("create order failed, recover redis stock and eligibility");
            remoteProductService.recoverRedisStockAndEligibility(order.getSourceId(), userId);
            redisService.deleteObject("order:status:" + order.getId());
        }else{
            log.info("order created successfully, orderId={}, amount={}", order.getId(), order.getAmount());

            StockDeductMessage msg = new StockDeductMessage();
            msg.setProductId(order.getSourceId());
            msg.setOrderId(order.getId());
            msg.setCount(order.getAmount() != null ? order.getAmount() : 1);

            mqMessageSendUtils.sendMqMessage(
                ProductMqConstants.PRODUCT_STOCK_EXCHANGE,
                ProductMqConstants.PRODUCT_STOCK_DEDUCT_ROUTING_KEY,
                msg,
                0
            );

            redisService.deleteObject("order:status:" + order.getId());
            mqMessageSendUtils.sendMqMessage( OrderMqConstants.ORDER_DELAY_EXCHANGE,OrderMqConstants.ORDER_DELAY_ROUTING_KEY,order.getId(),(OrderMqConstants.DELAY_TIME));
        }
    }

    /**
     * 统一处理订单创建入口。
     *
     * @param order 订单信息
     */
    public void handleOrder(Order order) {
        createOrder(order);
    }

    /**
     * 增加商品销量统计数据。
     *
     * @param order 订单信息
     */
    private void incrementSales(Order order) {
        if (order == null || order.getSourceId() == null) {
            return;
        }

        String productCountKey = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix() + order.getSourceId();
        if (Boolean.FALSE.equals(redisService.hasKey(productCountKey))) {
            initSalesCount(SalesTypeEnum.PRODUCT_SALES, order.getSourceId());
        }
        long amount = order.getAmount() != null ? order.getAmount() : 1;
        redisService.incrementCacheValue(productCountKey, amount);
        redisService.setCacheSet(SalesTypeEnum.PRODUCT_SALES.getDirtyKey(), order.getSourceId().toString());
    }

    /**
     * 增加店铺销量统计数据。
     *
     * @param order 订单信息
     */
    private void incrementShopSales(Order order) {
        log.info("开始更新店铺销量统计，order={}", order);
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
     * 回滚销量统计数据。
     *
     * @param order 订单信息
     * @param decrementShop 是否同步回滚店铺销量
     */
    private void decrementSales(Order order, boolean decrementShop) {
        if (order == null || order.getSourceId() == null) {
            return;
        }
        long amount = order.getAmount() != null ? order.getAmount() : 1;

        String productCountKey = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix() + order.getSourceId();
        if (Boolean.TRUE.equals(redisService.hasKey(productCountKey))) {
            redisService.decrementCacheValue(productCountKey, amount);
            redisService.setCacheSet(SalesTypeEnum.PRODUCT_SALES.getDirtyKey(), order.getSourceId().toString());
        }

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
     * 初始化销量统计缓存。
     *
     * @param type 统计类型（商品或店铺）
     * @param id 业务ID
     */
    private void initSalesCount(SalesTypeEnum type, Long id) {
        Integer baseline = 0;
        Integer totalOrders = 0;
        String countKey = type.getCountKeyPrefix() + id;

        if (type == SalesTypeEnum.PRODUCT_SALES) {
            baseline = remoteProductService.getSold(id);
            totalOrders = orderMapper.sumSoldBySourceId(id);
        } else if (type == SalesTypeEnum.SHOP_SALES) {
            baseline = remoteShopService.getSold(id);
            totalOrders = orderMapper.sumSoldByShopId(id);
        }

        int initialCount = (baseline != null ? baseline : 0) + (totalOrders != null ? totalOrders : 0);
        redisService.setCacheObject(countKey, initialCount);
    }

    /**
     * 分页查询当前用户的订单列表。
     *
     * @param order 查询条件
     * @param current 当前页码
     * @return 订单列表
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
                orderVO.setRules(product.getRulesJson()); // 将商品规则映射到订单规则字段
                orderVO.setPayValue(product.getPrice());      // 将商品售价映射到支付金额字段
                orderVO.setActualValue(product.getOriginalPrice()); // 将商品原价映射到实际价值字段
                orderVO.setTitle(product.getName());          // 将商品名称映射到订单标题字段
                orderVO.setSubTitle(product.getSubTitle());
                orderVO.setCoverImg(product.getCoverImg());
            }
            orderVOList.add(orderVO);
        });
        return orderVOList;
    }

    /**
     * 余额支付订单。
     *
     * @param id 订单ID
     * @return 影响行数
     */
    @Override
    public Integer pay(Long id) {
        Order order = getById(id);
        if(order==null){

            throw new BusinessException("order not found");
        }
        order.setPayTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.PAID);
        order.setPayType(PayTypeConstants.BALANCE);
        updateExpireTimeAfterPayment(order);
        incrementSales(order);
        return updateOrder(order);
    }

    /**
     * 支付成功回调处理。
     *
     * @param orderId 订单ID
     * @param payType 支付方式
     * @return 影响行数
     */
    @Override
    public Integer paySuccess(Long orderId, Integer payType) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException("order not found");
        }
        if (order.getStatus() == OrderStatusConstants.PAID) {
            return 1;
        }
        order.setPayTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.PAID);
        order.setPayType(payType);
        updateExpireTimeAfterPayment(order);
        incrementSales(order);
        return updateOrder(order);
    }

    /**
     * 支付后根据商品有效期信息更新订单有效期。
     *
     * @param order 订单信息
     */
    private void updateExpireTimeAfterPayment(Order order) {
        if (order == null || order.getSourceId() == null) {
            return;
        }
        ProductDTO productDTO = remoteProductService.getProductById(order.getSourceId());
        if (productDTO != null && productDTO.getValidityType() != null) {
            Date now = DateUtils.getNowDate();
            if (productDTO.getValidityType() == 2 && productDTO.getValidDays() != null) {
                order.setValidStartTime(now);
                order.setExpireTime(DateUtils.addDays(now, productDTO.getValidDays()));
            }
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
     * 取消订单并处理库存与退款逻辑。
     *
     * @param id 订单ID
     * @return 影响行数
     */
    @Override
    public Integer cancel(Long id) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("order not found");
        }
        Integer oldStatus = order.getStatus();
        order.setStatus(OrderStatusConstants.CANCELLED);
        int i = updateOrder(order);
        if(i>0){
            ProductDTO vo = remoteProductService.getProductById(order.getSourceId());
            if (vo != null && vo.getActivityType() != null && vo.getActivityType() == 1&&vo.getStatus().equals(ProductStatusEnum.ON_SHELF.getCode())){
                log.info("cancel order recovers stock for product activity");
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                decrementSales(order, false);
                sendRefundMessage(order);
            }
        }
        return i;
    }

    /**
     * 订单退款处理。
     *
     * @param id 订单ID
     * @return 影响行数
     */
    @Override
    public Integer refund(Long id) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("order not found");
        }
        Integer oldStatus = order.getStatus();
        order.setRefundTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.REFUNDED);
        int i = updateOrder(order);
        if(i>0){
            ProductDTO vo = remoteProductService.getProductById(order.getSourceId());
            if (vo != null && vo.getActivityType() != null && vo.getActivityType() == 1&&vo.getStatus().equals(ProductStatusEnum.ON_SHELF.getCode())){
                log.info("refund order recovers stock for product activity");
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                boolean wasVerified = (oldStatus == OrderStatusConstants.VERIFIED);
                decrementSales(order, wasVerified);
                sendRefundMessage(order);
            }
        }
        return i;
    }

    /**
     * 发送退款消息到MQ。
     *
     * @param order 订单信息
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
        log.info("refund message sent, orderId={}, userId={}, amount={}", order.getId(), order.getUserId(), order.getPayAmount());
    }

    /**
     * 核销/使用订单并触发积分消息。
     *
     * @param id 订单ID
     * @param verifyShopId 核销门店ID
     * @return 影响行数
     */
    @Override
    public Integer use(Long id, Long verifyShopId) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("order not found");
        }
        if (verifyShopId == null) {
            throw new BusinessException("verify shop id is required");
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
                throw new BusinessException("verify shop is not allowed for this order");
            }
        }
        order.setUseTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.VERIFIED);
        order.setVerifyShopId(verifyShopId);
        int i = updateOrder(order);
        if (i > 0) {
            incrementShopSales(order);
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
                log.info("发送积分消息成功，orderId={}, userId={}, payAmount={}", order.getId(), order.getUserId(), order.getPayAmount());
            }
        }
        return i;
    }

    /**
     * 获取用户订单总数。
     *
     * @param userId 用户ID
     * @return 订单数量
     */
    @Override
    public Integer getOrderCount(Long userId) {
        return query().eq("user_id", userId).count().intValue();
    }

    /**
     * 获取全部订单总数。
     *
     * @return 订单总数
     */
    @Override
    public Integer getOrderTotal() {
        return query().count().intValue();
    }
    /**
     * 根据订单ID获取订单详情VO。
     *
     * @param id 订单ID
     * @return 订单视图对象
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
     * 更新订单评价状态。
     *
     * @param orderId 订单ID
     * @param reviewId 评价ID
     * @param reviewTime 评价时间
     * @return 影响行数
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

    /**
     * 获取订单创建状态，供前端轮询。
     *
     * @param id 订单ID
     * @return 状态字符串
     */
    @Override
    public String getOrderStatus(Long id) {
        String key = "order:status:" + id;
        if (redisService.hasKey(key)) {
            return "PENDING";
        }

        Order order = getById(id);
        if (order != null) {
            return "SUCCESS";
        }

        return "FAILED";
    }

    /**
     * 统计商品销量。
     *
     * @return 商品销量列表
     */
    @Override
    public List<ProductSoldVO> countProductSold() {
        return orderMapper.countProductSold();
    }

    /**
     * 统计店铺近一周核销订单数。
     *
     * @param shopId 店铺ID
     * @return 订单数量
     */
    @Override
    public Integer countWeekOrders(Long shopId) {
        if (shopId == null) {
            return 0;
        }
        Integer count = orderMapper.countWeekOrders(shopId);
        return count == null ? 0 : count;
    }
    /**
     * 获取店铺订单分析数据。
     *
     * @param shopId 店铺ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 店铺订单分析
     */
    @Override
    public ShopOrderAnalysisVO getShopOrderAnalysis(Long shopId, String startTime, String endTime) {
        if (shopId == null) {
            return buildEmptyOrderAnalysis();
        }
        java.time.LocalDateTime[] timeRange = parseBusinessTimeRange(startTime, endTime);
        ShopOrderAnalysisVO analysis = orderMapper.selectShopOrderAnalysis(shopId, BUSINESS_ORDER_STATUSES, timeRange[0], timeRange[1]);
        if (analysis == null) {
            analysis = buildEmptyOrderAnalysis();
        }
        analysis.setHotProducts(fillProductNames(orderMapper.selectShopHotProducts(shopId, BUSINESS_ORDER_STATUSES, timeRange[0], timeRange[1], 3)));
        normalizeOrderAnalysis(analysis);
        return analysis;
    }

    /**
     * 获取店铺经营建议数据。
     *
     * @param shopId 店铺ID
     * @param timeRange 时间范围标识
     * @return 店铺经营建议
     */
    @Override
    public ShopOrderSuggestVO getShopOrderSuggest(Long shopId, String timeRange) {
        if (shopId == null) {
            return buildEmptyOrderSuggest();
        }
        LocalDateTime[] suggestRange = buildOrderSuggestRange(timeRange);
        ShopOrderAnalysisVO analysis = orderMapper.selectShopOrderAnalysis(shopId, BUSINESS_ORDER_STATUSES, suggestRange[0], suggestRange[1]);
        ShopOrderSuggestVO suggest = buildEmptyOrderSuggest();
        suggest.setWeekOrders(analysis == null || analysis.getTotalOrders() == null ? 0 : analysis.getTotalOrders());
        suggest.setHotProducts(fillProductNames(orderMapper.selectShopHotProducts(shopId, BUSINESS_ORDER_STATUSES, suggestRange[0], suggestRange[1], 3)));
        suggest.setSlowProducts(fillProductNames(orderMapper.selectShopSlowProducts(shopId, BUSINESS_ORDER_STATUSES, suggestRange[0], suggestRange[1], 3)));
        normalizeOrderSuggest(suggest);
        return suggest;
    }

    /**
     * 获取店铺复购率。
     *
     * @param shopId 店铺ID
     * @param timeRange 时间范围标识
     * @return 复购率
     */
    @Override
    public java.math.BigDecimal getShopRepurchaseRate(Long shopId, String timeRange) {
        if (shopId == null) {
            return java.math.BigDecimal.ZERO;
        }
        java.time.LocalDateTime[] range = buildOptionalOrderRange(timeRange);
        java.math.BigDecimal repurchaseRate = orderMapper.selectShopRepurchaseRate(
                shopId,
                BUSINESS_ORDER_STATUSES,
                range == null ? null : range[0],
                range == null ? null : range[1]);
        return repurchaseRate == null ? java.math.BigDecimal.ZERO : repurchaseRate;
    }

    /**
     * 解析并校验业务时间范围。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间范围数组
     */
    private java.time.LocalDateTime[] parseBusinessTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            throw new BusinessException("startTime and endTime are required");
        }
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime, BUSINESS_TIME_FORMATTER);
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime, BUSINESS_TIME_FORMATTER);
            if (end.isBefore(start)) {
                throw new BusinessException("endTime must be greater than or equal to startTime");
            }
            return new java.time.LocalDateTime[]{start, end};
        } catch (java.time.format.DateTimeParseException ex) {
            throw new BusinessException("invalid time range format");
        }
    }

    /**
     * 构建经营建议统计时间范围。
     *
     * @param timeRange 时间范围标识
     * @return 时间范围数组
     */
    private java.time.LocalDateTime[] buildOrderSuggestRange(String timeRange) {
        String normalized = timeRange == null || timeRange.isBlank() ? "week" : timeRange.trim().toLowerCase(java.util.Locale.ROOT);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return switch (normalized) {
            case "month" -> new java.time.LocalDateTime[]{now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now};
            case "quarter" -> new java.time.LocalDateTime[]{now.minusDays(90), now};
            case "week" -> new java.time.LocalDateTime[]{now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(), now};
            default -> throw new BusinessException("unsupported timeRange");
        };
    }

    /**
     * 构建可选时间范围，不传参则返回空。
     *
     * @param timeRange 时间范围标识
     * @return 时间范围数组或空
     */
    private java.time.LocalDateTime[] buildOptionalOrderRange(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) {
            return null;
        }
        return buildOrderSuggestRange(timeRange);
    }

    /**
     * 批量回填商品销量列表中的商品名称。
     */
    private List<ProductSalesVO> fillProductNames(List<ProductSalesVO> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> productIds = products.stream()
                .map(ProductSalesVO::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return products;
        }
        List<ProductDTO> productList = remoteProductService.getProductListByIds(productIds);
        Map<Long, String> productNameMap = productList == null ? new java.util.HashMap<>() : productList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProductDTO::getId, product -> product.getName() == null ? "" : product.getName(), (left, right) -> left));
        products.forEach(product -> {
            product.setProductName(productNameMap.getOrDefault(product.getProductId(), ""));
            if (product.getSalesCount() == null) {
                product.setSalesCount(0L);
            }
        });
        return products;
    }

    /**
     * 批量回填订单中的商品名称。
     */
    private void fillOrderProductNames(List<OrderVO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> productIds = orders.stream()
                .map(OrderVO::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return;
        }
        List<ProductDTO> productList = remoteProductService.getProductListByIds(productIds);
        Map<Long, String> productNameMap = productList == null ? new HashMap<>() : productList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProductDTO::getId, product -> product.getName() == null ? "" : product.getName(), (left, right) -> left));
        orders.forEach(order -> order.setProductName(productNameMap.getOrDefault(order.getSourceId(), "")));
    }

    /**
     * 批量回填订单中的店铺名称。
     */
    private void fillOrderShopNames(List<OrderVO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> shopIds = orders.stream()
                .flatMap(order -> collectOrderShopIds(order).stream())
                .distinct()
                .toList();
        if (shopIds.isEmpty()) {
            return;
        }
        List<ShopDTO> shopList = remoteShopService.getShopList(shopIds);
        Map<Long, String> shopNameMap = shopList == null ? new HashMap<>() : shopList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ShopDTO::getId, shop -> shop.getName() == null ? "" : shop.getName(), (left, right) -> left));
        orders.forEach(order -> {
            List<Long> orderShopIds = collectOrderShopIds(order);
            if (!orderShopIds.isEmpty()) {
                String shopNames = orderShopIds.stream()
                        .map(shopNameMap::get)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(","));
                if (StringUtils.hasText(shopNames)) {
                    order.setShopName(shopNames);
                    return;
                }
            }
            if (order.getVerifyShopId() != null) {
                order.setShopName(shopNameMap.getOrDefault(order.getVerifyShopId(), ""));
            }
        });
    }

    /**
     * 批量回填订单中的用户昵称。
     */
    private void fillOrderUserNames(List<OrderVO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> userIds = orders.stream()
                .map(OrderVO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        List<UserDTO> userList = remoteAppUserService.getUserList(userIds);
        Map<Long, String> userNameMap = userList == null ? new HashMap<>() : userList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserDTO::getId, user -> user.getNickName() == null ? "" : user.getNickName(), (left, right) -> left));
        orders.forEach(order -> order.setUserName(userNameMap.getOrDefault(order.getUserId(), "")));
    }

    /**
     * 解析订单可用店铺ID集合。
     *
     * @param order 订单信息
     * @return 店铺ID列表
     */
    private List<Long> collectOrderShopIds(OrderVO order) {
        List<Long> shopIds = new ArrayList<>();
        if (order == null) {
            return shopIds;
        }
        if (StringUtils.hasText(order.getShopId())) {
            Arrays.stream(order.getShopId().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(shopId -> {
                        try {
                            shopIds.add(Long.valueOf(shopId));
                        } catch (NumberFormatException ignored) {
                        }
                    });
        }
        if (shopIds.isEmpty() && order.getVerifyShopId() != null) {
            shopIds.add(order.getVerifyShopId());
        }
        return shopIds;
    }

    /**
     * 构建空的订单分析对象。
     *
     * @return 订单分析对象
     */
    private ShopOrderAnalysisVO buildEmptyOrderAnalysis() {
        return new ShopOrderAnalysisVO(0, java.math.BigDecimal.ZERO, 0, new ArrayList<>());
    }

    /**
     * 构建空的订单建议对象。
     *
     * @return 订单建议对象
     */
    private ShopOrderSuggestVO buildEmptyOrderSuggest() {
        return new ShopOrderSuggestVO(0, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * 规整订单分析数据的默认值。
     *
     * @param analysis 订单分析对象
     */
    private void normalizeOrderAnalysis(ShopOrderAnalysisVO analysis) {
        if (analysis.getTotalOrders() == null) {
            analysis.setTotalOrders(0);
        }
        if (analysis.getTotalRevenue() == null) {
            analysis.setTotalRevenue(java.math.BigDecimal.ZERO);
        }
        if (analysis.getRepurchaseCount() == null) {
            analysis.setRepurchaseCount(0);
        }
        if (analysis.getHotProducts() == null) {
            analysis.setHotProducts(new ArrayList<>());
        }
    }

    /**
     * 规整订单建议数据的默认值。
     *
     * @param suggest 订单建议对象
     */
    private void normalizeOrderSuggest(ShopOrderSuggestVO suggest) {
        if (suggest.getWeekOrders() == null) {
            suggest.setWeekOrders(0);
        }
        if (suggest.getHotProducts() == null) {
            suggest.setHotProducts(new ArrayList<>());
        }
        if (suggest.getSlowProducts() == null) {
            suggest.setSlowProducts(new ArrayList<>());
        }
    }

    /**
     * 订单过期处理。
     *
     * @param id 订单ID
     * @return 影响行数
     */
    @Override
    public Integer expired(Long id) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("order not found");
        }
        Integer oldStatus = order.getStatus();
        order.setStatus(OrderStatusConstants.EXPIRED);
        int i = updateOrder(order);
        if(i>0){
            ProductDTO vo = remoteProductService.getProductById(order.getSourceId());
            if (vo != null && vo.getActivityType() != null && vo.getActivityType() == 1&&vo.getStatus().equals(ProductStatusEnum.ON_SHELF.getCode())){
                log.info("expired order recovers stock for product activity");
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                decrementSales(order, false);
                sendRefundMessage(order);
            }
        }
        return i;
    }
}
