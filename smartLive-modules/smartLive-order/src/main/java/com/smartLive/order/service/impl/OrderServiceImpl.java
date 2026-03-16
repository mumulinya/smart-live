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

    @Override
    public Order selectOrderById(Long id)
    {

        return orderMapper.selectOrderById(id);
    }

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

    @Override
    public int insertOrder(Order order)
    {
        order.setCreateTime(DateUtils.getNowDate());
        return orderMapper.insertOrder(order);
    }

    @Override
    public int updateOrder(Order order)
    {
        order.setUpdateTime(DateUtils.getNowDate());
        return orderMapper.updateOrder(order);
    }

    @Override
    public int deleteOrderByIds(Long[] ids)
    {
        return orderMapper.deleteOrderByIds(ids);
    }

    @Override
    public int deleteOrderById(Long id)
    {
        return orderMapper.deleteOrderById(id);
    }

    public  void createOrder(Order order) {
        Long userId = order.getUserId();
        Integer count = query()
                .eq("user_id", userId)
                .eq("source_id", order.getSourceId())
                .notIn("status",
                        OrderStatusConstants.EXPIRED,   // 闂傚倸鍊风粈渚€骞栭位鍥敃閿曗偓閻ょ偓绻涢幋鐐╂（婵炲樊浜濋弲婵嬫煃瑜滈崜鐔煎极?
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
    
    public void handleOrder(Order order) {
        createOrder(order);
    }

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

    private void incrementShopSales(Order order) {
        log.info("闂傚倸鍊风欢锟犲磻閸涱垱鏆滈柟鐑橆樄閻戣棄绀冮柍鍝勫€搁崝鍛攽鎺抽崐鏇㈠箠韫囨稑绠犳繛鍡樻尰閳锋垿骞栫€涙ɑ灏ù婊堢畺濮婄粯鎷呴悷鎵シ缂備胶绮敮鐐参ｉ幇鐗堟櫆闂佹鍨版禍鐐箾閹寸儐浠炬い蹇撶吇閸ヮ剙鐓涢柛娑卞枛娴? order={}", order);
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
                // 闂傚倸鍊搁崐宄懊归崶顒夋晪闁哄稁鍘奸崒銊ф喐閻楀牆绗掗柛銊ュ€婚幉鎼佹偋閸繂鎯為梺鎼炲労閸撴瑩鎯屽Δ鈧…璺ㄦ崉娓氼垰鍓伴梺鍛婃煥閹虫ê顫忛搹瑙勫枂闁告洦鍋勬慨銏㈢磼閸撗嗘闁告瑥鍟撮悰?
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                decrementSales(order, false);
                sendRefundMessage(order);
            }
        }
        return i;
    }

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
                log.info("闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹寸偛顕滅紒浣哄閵囧嫰骞樺畷鍥┬ㄩ悗瑙勬穿缁绘繂顕ｉ幘顔藉亜闁告繂瀚峰Λ搴ㄦ⒒閸屾瑧顦︾紒銊╀憾瀹曟垿骞樼紒妯煎幘婵°倧绲介崰姘涢幋鐘冲仏鐟滄棃寮诲☉銏犵闁瑰鍋為崕妤哄┑鐘垫暩閸嬬偤宕归悽鍛婂亱濠电姴鍟? orderId={}, userId={}, payAmount={}", order.getId(), order.getUserId(), order.getPayAmount());
            }
        }
        return i;
    }

    @Override
    public Integer getOrderCount(Long userId) {
        int orderCount = query().eq("user_id", userId).count().intValue();
        System.out.println("闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺缁樻尰濞茬喖骞冪憴鍕闂傚牊绋撴禒濂告倵鐟欏嫭绀堥柛鐘崇墵閻涱噣鍩€椤掑倻纾藉ù锝堝亗閹存績鏋?"+orderCount);
        return orderCount;
    }

    @Override
    public Integer getOrderTotal() {
        return query().count().intValue();
    }
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

    private ShopOrderAnalysisVO buildEmptyOrderAnalysis() {
        return new ShopOrderAnalysisVO(0, java.math.BigDecimal.ZERO, 0, new ArrayList<>());
    }

    private ShopOrderSuggestVO buildEmptyOrderSuggest() {
        return new ShopOrderSuggestVO(0, new ArrayList<>(), new ArrayList<>());
    }

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
                // 闂傚倸鍊搁崐宄懊归崶顒夋晪闁哄稁鍘奸崒銊ф喐閻楀牆绗掗柛銊ュ€婚幉鎼佹偋閸繂鎯為梺鎼炲労閸撴瑩鎯屽Δ鈧…璺ㄦ崉娓氼垰鍓伴梺鍛婃煥閹虫ê顫忛搹瑙勫枂闁告洦鍋勬慨銏㈢磼閸撗嗘闁告瑥鍟撮悰?
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
