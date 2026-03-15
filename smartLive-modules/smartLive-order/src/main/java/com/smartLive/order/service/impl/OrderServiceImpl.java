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
 * 鐠併垹宕熺悰鈯縠rvice娑撴艾濮熺仦鍌氼槱閻?
 *
 * @author mumulin
 * @date 2025-09-21
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


    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private RedisService redisService;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    /**
     * 闁插﹥鏂侀柨浣藉壖閺堫剙鍨垫慨瀣
     */
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    private IOrderService proxy;

    /**
     * 閺屻儴顕楃拋銏犲礋鐞?
     *
     * @param id 鐠併垹宕熺悰銊ゅ瘜闁?
     * @return 鐠併垹宕熺悰?
     */
    @Override
    public Order selectOrderById(Long id)
    {

        return orderMapper.selectOrderById(id);
    }

    /**
     * 閺屻儴顕楃拋銏犲礋鐞涖劌鍨悰?
     *
     * @param order 鐠併垹宕熺悰?
     * @return 鐠併垹宕熺悰?
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
     * 閺傛澘顤冪拋銏犲礋鐞?
     *
     * @param order 鐠併垹宕熺悰?
     * @return 缂佹挻鐏?
     */
    @Override
    public int insertOrder(Order order)
    {
        order.setCreateTime(DateUtils.getNowDate());
        return orderMapper.insertOrder(order);
    }

    /**
     * 娣囶喗鏁肩拋銏犲礋鐞?
     *
     * @param order 鐠併垹宕熺悰?
     * @return 缂佹挻鐏?
     */
    @Override
    public int updateOrder(Order order)
    {
        order.setUpdateTime(DateUtils.getNowDate());
        return orderMapper.updateOrder(order);
    }

    /**
     * 閹靛綊鍣洪崚鐘绘珟鐠併垹宕熺悰?
     *
     * @param ids 闂団偓鐟曚礁鍨归梽銈囨畱鐠併垹宕熺悰銊ゅ瘜闁?
     * @return 缂佹挻鐏?
     */
    @Override
    public int deleteOrderByIds(Long[] ids)
    {
        return orderMapper.deleteOrderByIds(ids);
    }

    /**
     * 閸掔娀娅庣拋銏犲礋鐞涖劋淇婇幁?
     *
     * @param id 鐠併垹宕熺悰銊ゅ瘜闁?
     * @return 缂佹挻鐏?
     */
    @Override
    public int deleteOrderById(Long id)
    {
        return orderMapper.deleteOrderById(id);
    }

    /**
     * 婢跺嫮鎮婄拋銏犲礋
     * @param order
     */

    public void handleOrder(Order order) {
        //閼惧嘲褰囨禍瀣娴狅絿鎮婄€电钖?
        proxy= (IOrderService) AopContext.currentProxy();

        //1閵嗗倽骞忛崣鏍暏閹寸d
        Long userId = order.getUserId();
        //2.閼惧嘲褰噐edisson闁夸礁顕挒?
        RLock lock = redissonClient.getLock("order:" + userId);
        //3.閼惧嘲褰囬柨?
        boolean isLock = lock.tryLock();
        //閸掋倖鏌囬柨浣规Ц閸氾箒骞忛崣鏍ㄥ灇閸?
        if(!isLock){
            //閼惧嘲褰囬柨浣搞亼鐠?鏉╂柨娲栭柨娆掝嚖娣団剝浼?
            log.error("failed to acquire order lock");
            return;
        }
        try {
            proxy.createOrder(order);
        }catch (Exception e){
            log.error("create order failed", e);
        }finally {
            //闁插﹥鏂侀柨?
            lock.unlock();
        }

    }

    /**
     *鐎圭偟骞囨稉鈧禍杞扮閸?
     * @param order
     * @return
     */
    public  void createOrder(Order order) {
        //閼惧嘲褰囪ぐ鎾冲閻劍鍩沬d
        Long userId = order.getUserId();
        //閸掋倖鏌囪ぐ鎾冲閻劍鍩涢弰顖氭儊鐠愵厺鎷辨潻?
        Integer count = query()
                .eq("user_id", userId)
                .eq("source_id", order.getSourceId())
                .notIn("status",
                        OrderStatusConstants.EXPIRED,   // 鏉╁洦婀?
                        OrderStatusConstants.CANCELLED, // 閸欐牗绉?
                        OrderStatusConstants.REFUNDED   // 闁偓濞?
                ).count().intValue();
        if(count>0){
            //閻劍鍩涘鑼病鐠愵厺鎷辨潻鍥︾啊
            log.error("閻劍鍩涘鑼病鐠愵厺鎷辨潻鍥︾啊閿涘矁袝閸?Redis 閸ョ偤鈧偓");
            // 閻楄鐣╂径鍕倞閿涙艾顩ч弸婊勬Ц缁夋帗娼冮崷鐑樻珯閿涘矁娅ч悞鑸垫殶閹诡喖绨遍幐鈥茬秶娴滃棴绱濇担?Lua 閼存碍婀伴崣顖濆厴瀹歌尙绮￠幍锝勭啊妫板嫬绨辩€涙﹫绱濇潻娆撳櫡鐏忔繆鐦崶鐐寸泊
            remoteProductService.recoverRedisStockAndEligibility(order.getSourceId(), null);
            redisService.deleteObject("order:status:" + order.getId());
            return;
        }
        // 7.閸掓稑缂撶拋銏犲礋
        boolean save = save(order);
        if(!save){
            //閸掓稑缂撴径杈Е
            log.error("閸掓稑缂撶拋銏犲礋娣囨繂鐡ㄩ弫鐗堝祦鎼存挸銇戠拹銉礉鐟欙箑褰?Redis 閸ョ偤鈧偓");
            remoteProductService.recoverRedisStockAndEligibility(order.getSourceId(), userId);
            redisService.deleteObject("order:status:" + order.getId());
        }else{
            log.info("鐠併垹宕熷鎻掑灡瀵ょ尨绱滻D={}閿涘本鏆熼柌?{}閿涘苯褰傞柅?MQ 瀵倹顒為幍锝呯氨閹稿洣鎶?..", order.getId(), order.getAmount());

            // 閺嬪嫬缂撻獮璺哄絺闁礁绱撳銉ョ氨鐎涙ɑ澧搁崙蹇旂Х閹垳绮伴崯鍡楁惂濡€虫健
            StockDeductMessage msg = new StockDeductMessage();
            msg.setProductId(order.getSourceId());
            msg.setOrderId(order.getId());
            msg.setCount(order.getAmount() != null ? order.getAmount() : 1); // 閹碉絽鍣虹€圭偤妾拹顓濇嫳閺佷即鍣?

            mqMessageSendUtils.sendMqMessage(
                ProductMqConstants.PRODUCT_STOCK_EXCHANGE,
                ProductMqConstants.PRODUCT_STOCK_DEDUCT_ROUTING_KEY,
                msg,
                0
            );

            // 閸掓稑缂撻幋鎰閿涘苯鍨归梽?Redis 閸楃姳缍呯粭?
            redisService.deleteObject("order:status:" + order.getId());
            // 閸欐垿鈧礁娆㈡潻鐔哥Х閹垽绱濆Λ鈧ù瀣吂閸楁洘鏁禒妯煎Ц閹?
            mqMessageSendUtils.sendMqMessage( OrderMqConstants.ORDER_DELAY_EXCHANGE,OrderMqConstants.ORDER_DELAY_ROUTING_KEY,order.getId(),(OrderMqConstants.DELAY_TIME));
        }
    }

    /**
     * 婢х偛濮為崯鍡楁惂閸欏﹤鍙剧€电懓绨叉惔妤呮懙閻ㄥ嫰鏀㈤柌?
     * 闁插洨鏁ら垾婊€绗佺痪褍娲栭柅鈧垾婵堢摜閻ｃ儰绻氱拠浣规殶閹诡喖鍨垫慨瀣閿?
     * 1. 濡偓閺?Redis 鐠佲剝鏆熼崳顭掔幢
     * 2. 閼?Redis 娑撹櫣鈹栭敍宀勨偓姘崇箖 Feign 鐠嬪啰鏁ゅ┃鎰侀崸妤嬬礄Product/Shop閿涘骞忛崣鏍у嚒閽€钘夌氨閺佹澘鈧》绱?
     * 3. 閼汇儲绨Ο鈥虫健閺佹澘鈧棿绮涙稉宥呭讲娣団剝鍨ㄦ稉鍝勫灥婵瀵查敍灞藉灟閸︺劍婀板Ο鈥虫健閺佺増宓佹惔鎾寸湽閹粯澧嶉張澶婂坊閸欒尪顓归崡鏇炵暚閹存劖鏆熼妴?
     * 
     * @param order 鐠併垹宕熺€圭偘缍?
     */
    private void incrementSales(Order order) {
        if (order == null || order.getSourceId() == null) {
            return;
        }

        // 婢跺嫮鎮婇崯鍡楁惂闁库偓闁插繒鐤崝?
        String productCountKey = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix() + order.getSourceId();
        if (Boolean.FALSE.equals(redisService.hasKey(productCountKey))) {
            initSalesCount(SalesTypeEnum.PRODUCT_SALES, order.getSourceId());
        }
        long amount = order.getAmount() != null ? order.getAmount() : 1;
        redisService.incrementCacheValue(productCountKey, amount);
        redisService.setCacheSet(SalesTypeEnum.PRODUCT_SALES.getDirtyKey(), order.getSourceId().toString());
    }

    /**
     * 婢х偛濮炵€电懓绨叉惔妤呮懙閻ㄥ嫰鏀㈤柌?(娴犲懎婀拋銏犲礋鐎圭偤妾悮顐ｇ壋闁库偓娴ｈ法鏁ら弮鎯扮殶閻?
     */
    private void incrementShopSales(Order order) {
        log.info("鐠併垹宕熷鍙夌壋闁库偓閿涘苯绱戞慨瀣柈閸旂姴绨甸柧娲敘闁?..{}", order);
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
     * 閸ョ偤鈧偓閸熷棗鎼ч崣濠傜暗闁炬椽鏀㈤柌?(鐠併垹宕熼崣鏍ㄧХ閹存牠鈧偓濞嗙偓妞傜拫鍐暏)
     *
     * @param order 鐠併垹宕熺€圭偘缍?
     * @param decrementShop 閺勵垰鎯侀棁鈧憰浣告倱閺冭泛娲栭柅鈧惔妤呮懙闁库偓闁?
     */
    private void decrementSales(Order order, boolean decrementShop) {
        if (order == null || order.getSourceId() == null) {
            return;
        }
        long amount = order.getAmount() != null ? order.getAmount() : 1;

        // 1. 閸ョ偤鈧偓閸熷棗鎼ч柨鈧柌?
        String productCountKey = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix() + order.getSourceId();
        if (Boolean.TRUE.equals(redisService.hasKey(productCountKey))) {
            redisService.decrementCacheValue(productCountKey, amount);
            redisService.setCacheSet(SalesTypeEnum.PRODUCT_SALES.getDirtyKey(), order.getSourceId().toString());
        }

        // 2. 閸ョ偤鈧偓鎼存鎽甸柨鈧柌?(娴犲懎缍嬬拋銏犲礋瀹歌尪顫﹂弽鎼佹敘閿涘奔绗栭張澶婎嚠鎼?shopId 閺?
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
     * 閸掓繂顫愰崠鏍敘闁插繗顓搁弫鏉挎珤閿涘牅绗佺痪褏鐡ラ悾銉礆
     */
    private void initSalesCount(SalesTypeEnum type, Long id) {
        Integer baseline = 0;
        Integer totalOrders = 0;
        String countKey = type.getCountKeyPrefix() + id;

        if (type == SalesTypeEnum.PRODUCT_SALES) {
            // 娴犲骸鏅㈤崫浣鼓侀崸妤勫箯閸欐牕鍑￠幐浣风畽閸栨牜娈戦柨鈧柌蹇撶唨閺?
            baseline = remoteProductService.getSold(id);
            // 娴犲氦顓归崡鏇炵氨閼惧嘲褰囬崗銊╁劥瀹歌尙鈥樼拋?閺€顖欑帛閻ㄥ嫯顓归崡鏇熲偓濠氬櫤閿涘牆鍘规惔鏇炲弿闁插繐鍨垫慨瀣閿?
            totalOrders = orderMapper.sumSoldBySourceId(id);
        } else if (type == SalesTypeEnum.SHOP_SALES) {
            // 娴犲骸绨甸柧鐑樐侀崸妤勫箯閸欐牕鍑￠幐浣风畽閸栨牜娈戦柨鈧柌蹇撶唨閺?
            baseline = remoteShopService.getSold(id);
            // 娴犲氦顓归崡鏇炵氨閼惧嘲褰囩拠銉ョ暗闁惧搫鍙忛柈銊╂敘闁?
            totalOrders = orderMapper.sumSoldByShopId(id);
        }

        // 闁槒绶敍姘洤閺嬫粌鐔€閺佹澘鍑＄紒蹇撳瘶閸氼偂绨＄拋銏犲礋鎼存挾娈戦崢鍡楀蕉閺佺増宓侀敍灞藉灟閻╁瓨甯撮悽銊ョ唨閺佸府绱?
        // 閸掓繂顫愰崠鏍ㄦ鎼存梻鈥樻穱?Redis 娑擃厽妲歌ぐ鎾冲閺堚偓閸戝棛鈥橀惃鍕┾偓鎰弿闁插繑鈧粯鏆熼妴鎴欌偓?
        // 閺嶈宓佺憰浣圭湴閿涙埃鈧粌鍘涢崢缁樼爱濡€虫健閼惧嘲褰囬弫鐗堝祦閿涘瞼鍔ч崥搴″晙閸樼粯婀板Ο鈥虫健閺屻儴顕楅垾婵撶礉鏉╂瑩鍣烽崣鏍﹁⒈閼板懍绠ｉ崪灞烩偓?
        int initialCount = (baseline != null ? baseline : 0) + (totalOrders != null ? totalOrders : 0);
        redisService.setCacheObject(countKey, initialCount);
    }

    /**
     * 閼惧嘲褰囪ぐ鎾冲閻劍鍩涚拋銏犲礋閸掓銆?
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
     * 閺€顖欑帛鐠併垹宕?
     *
     * @param id
     * @param
     * @return
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
        // 閺€顖欑帛閹存劕濮涢敍宀€鐤崝鐘叉櫌閸濅線鏀㈤柌蹇曠埠鐠?
        incrementSales(order);
        return updateOrder(order);
    }

    /**
     * 閺€顖欑帛閹存劕濮涢弴瀛樻煀鐠併垹宕熼悩鑸碘偓渚婄礄閸愬懘鍎寸拫鍐暏閿涘本鏁幐浣割樋缁夊秵鏁禒妯绘煙瀵骏绱?
     *
     * @param orderId 鐠併垹宕烮D
     * @param payType 閺€顖欑帛閺傜懓绱? 1=娴ｆ瑩顤?2=閺€顖欑帛鐎?3=瀵邦喕淇?
     * @return 瑜板崬鎼风悰灞炬殶
     */
    @Override
    public Integer paySuccess(Long orderId, Integer payType) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException("order not found");
        }
        // 楠炲倻鐡戦弽锟犵崣閿涙艾鍑￠弨顖欑帛閸掓瑨鐑︽潻?
        if (order.getStatus() == OrderStatusConstants.PAID) {
            return 1;
        }
        order.setPayTime(DateUtils.getNowDate());
        order.setStatus(OrderStatusConstants.PAID);
        order.setPayType(payType);
        updateExpireTimeAfterPayment(order);
        // 閺€顖欑帛閹存劕濮涢敍宀€鐤崝鐘叉櫌閸濅線鏀㈤柌蹇曠埠鐠?
        incrementSales(order);
        return updateOrder(order);
    }

    /**
     * 閺€顖欑帛閹存劕濮涢崥搴礄閹存牔绮▎鍓х仜闂傝揪绱氶敍灞艰礋閻楃懓鐣鹃崯鍡楁惂缁鐎风拋锛勭暬閻喐顒滈惃鍕箒閺佸牊婀￠幋顏咁剾閺冨爼妫块妴?
     * 闁灝鍘ら崶鐘虫暜娴犳ê鈧帟顓搁弮璁圭礄婵?5閸掑棝鎸撻敍澶婎嚤閼峰鏁ら幋铚傜碍閹圭喍濞囬悽銊︽埂閵?
     *
     * @param order 鐠併垹宕?
     */
    private void updateExpireTimeAfterPayment(Order order) {
        if (order == null || order.getSourceId() == null) {
            return;
        }
        ProductDTO productDTO = remoteProductService.getProductById(order.getSourceId());
        if (productDTO != null && productDTO.getValidityType() != null) {
            Date now = DateUtils.getNowDate();
            // 閳ユ粏鍠樻稊鏉挎倵 N 婢垛晛鍞撮張澶嬫櫏閳ユ繐绱伴弨顖欑帛閹存劕濮涙潻娆庣閸掓槒鎹ｇ粻?
            if (productDTO.getValidityType() == 2 && productDTO.getValidDays() != null) {
                order.setValidStartTime(now);
                order.setExpireTime(DateUtils.addDays(now, productDTO.getValidDays()));
            }
            // 閸ュ搫鐣鹃張澶嬫櫏閺?
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
     * 閸欐牗绉风拋銏犲礋
     *
     * @param id
     * @param
     * @return
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
                // 閹垹顦叉惔鎾崇摠
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            // 鐠併垹宕熼崣鏍ㄧХ閵嗗倸顩ч弸婊勬Ц瀹稿弶鏁禒妯款吂閸楁洖褰囧☉鍫礄娓氬顩х粻锛勬倞閸涙ɑ鎼锋担婊愮礆閿涘苯鍨崶鐐衡偓鈧崯鍡楁惂闁库偓闁?(閺堫亝鐗抽柨鈧潻鍥风礉娑撳秹娓剁憰浣告礀闁偓鎼存鎽甸柨鈧柌?
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                decrementSales(order, false);
                // 閸欐垿鈧府Q濞戝牊浼呴柅姘辩叀闁藉崬瀵樺Ο鈥虫健閿涘苯鐨㈤柅鈧▎楣冨櫨妫版繈鈧偓閸ョ偛鍩岄悽銊﹀煕娴ｆ瑩顤?
                sendRefundMessage(order);
            }
        }
        return i;
    }

    /**
     * 闁偓濞嗘崘顓归崡?
     *
     * @param id
     * @param
     * @return
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
            // 鐠併垹宕熼柅鈧▎淇扁偓鍌氼洤閺嬫粍妲稿鍙夋暜娴犳顓归崡鏇礄status >= PAID閿涘鈧偓濞嗘拝绱濋崶鐐寸泊閸熷棗鎼ч柨鈧柌蹇嬧偓鍌濆婢跺嫪绨鍙夌壋闁库偓閻樿埖鈧線鈧偓濞嗘拝绱濋崥灞炬閸ョ偤鈧偓鎼存鎽甸柨鈧柌?
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                boolean wasVerified = (oldStatus == OrderStatusConstants.VERIFIED);
                decrementSales(order, wasVerified);
                // 閸欐垿鈧府Q濞戝牊浼呴柅姘辩叀闁藉崬瀵樺Ο鈥虫健閿涘苯鐨㈤柅鈧▎楣冨櫨妫版繈鈧偓閸ョ偛鍩岄悽銊﹀煕娴ｆ瑩顤?
                sendRefundMessage(order);
            }
        }
        return i;
    }

    /**
     * 閸欐垿鈧線鈧偓濞嗙偓绉烽幁顖氬煂闁藉崬瀵樺Ο鈥虫健閿涘牊澧嶉張澶嬫暜娴犳ɑ鏌熷蹇撴綆闁偓濞嗘崘鍤︽担娆擃杺閿?
     *
     * @param order 鐠併垹宕?
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
        log.info("瀹告彃褰傞柅渚€鈧偓濞嗙锭Q濞戝牊浼? orderId={}, userId={}, amount={}", order.getId(), order.getUserId(), order.getPayAmount());
    }

    /**
     * 娴ｈ法鏁ょ拋銏犲礋 (閺嶆悂鏀?
     *
     * @param id 鐠併垹宕烮D
     * @param verifyShopId 閺嶆悂鏀㈤惃鍕，鎼存“D
     * @return 瑜板崬鎼风悰灞炬殶
     */
    @Override
    public Integer use(Long id, Long verifyShopId) {
        Order order = getById(id);
        if(order==null){
            throw new BusinessException("order not found");
        }
        if (verifyShopId == null) {
            throw new BusinessException("閺嶆悂鏀㈤梻銊ョ暗娑撳秷鍏樻稉铏光敄");
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
            // 閺嶆悂鏀㈤幋鎰閿涘苯顤冮崝鐘绘，鎼存鏀㈤柌?
            incrementShopSales(order);
            // 鐠併垹宕熼弽鎼佹敘閹存劕濮涢敍灞藉絺闁府Q濞戝牊浼呭鍌涱劄婵傛牕濮崇粔顖氬瀻閿涘牏袧閸?= 鐎圭偘绮柌鎴︻杺閿?
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
                log.info("瀹告彃褰傞柅浣盒濋崚鍡楊殯閸旂洝Q濞戝牊浼? orderId={}, userId={}, payAmount={}", order.getId(), order.getUserId(), order.getPayAmount());
            }
        }
        return i;
    }

    /**
     * 閼惧嘲褰囩拋銏犲礋閺佷即鍣?
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getOrderCount(Long userId) {
        int orderCount = query().eq("user_id", userId).count().intValue();
        System.out.println("鐠併垹宕熼弫浼村櫤娑?"+orderCount);
        return orderCount;
    }

    /**
     * 閼惧嘲褰囩拋銏犲礋閹粯鏆?
     *
     * @return
     */
    @Override
    public Integer getOrderTotal() {
        return query().count().intValue();
    }
    /**
     * 閺嶈宓乮d閺屻儴顕楃拋銏犲礋
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
     * 娣囶喗鏁肩拋銏犲礋鐠囧嫪鐜悩鑸碘偓?
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
        // 1. 閸忓牊鐓?Redis 闁插瞼娈戦弽鍥槕 key (e.g. "order:status:" + id)
        //    閸嬪洩顔曢崜宥囶伂娴肩姾绻冮弶銉ф畱 id 閸忚泛鐤勯弰?snowflake id 閹存牞鈧懏鐓囩粔宥勭瑹閸?id,
        //    閸︺劌鍨卞楦款吂閸楁洖澧犻敍灞藉嚒缂佸繒鏁遍崜宥囶伂閹存牜缍夐崗宕囨晸閹存劕鑻熺€涙ê鍙?Redis marked as "CREATING"
        String key = "order:status:" + id;
        if (redisService.hasKey(key)) {
            return "PENDING";
        }

        // 2. Redis 濞?key 娴滃棴绱濈拠瀛樻鐟曚椒绠炴径杈Е鐟曚椒绠為幋鎰閿涘本鐓￠弫鐗堝祦鎼?
        Order order = getById(id);
        if (order != null) {
            return "SUCCESS";
        }

        // 3. 閺冦垺鐥?key 娑旂喐鐥呮惔鎾诡唶瑜?-> 婢惰精瑙?
        return "FAILED";
    }

    /**
     * 閼惧嘲褰囬崯鍡楁惂闁库偓閸烆喚绮虹拋?
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

    /**
     * 鐠併垹宕熸潻鍥ㄦ埂
     *
     * @param id
     * @return
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
                // 閹垹顦叉惔鎾崇摠
                remoteProductService.recoverStock(order.getSourceId(),order.getUserId());
            }
            // 鐠併垹宕熼崣鏍ㄧХ閵嗗倸顩ч弸婊勬Ц瀹稿弶鏁禒妯款吂閸楁洖褰囧☉鍫礄娓氬顩х粻锛勬倞閸涙ɑ鎼锋担婊愮礆閿涘苯鍨崶鐐衡偓鈧崯鍡楁惂闁库偓闁?(閺堫亝鐗抽柨鈧潻鍥风礉娑撳秹娓剁憰浣告礀闁偓鎼存鎽甸柨鈧柌?
            if (oldStatus != null && oldStatus >= OrderStatusConstants.PAID) {
                decrementSales(order, false);
                // 閸欐垿鈧府Q濞戝牊浼呴柅姘辩叀闁藉崬瀵樺Ο鈥虫健閿涘苯鐨㈤柅鈧▎楣冨櫨妫版繈鈧偓閸ョ偛鍩岄悽銊﹀煕娴ｆ瑩顤?
                sendRefundMessage(order);
            }
        }
        return i;
    }
}
