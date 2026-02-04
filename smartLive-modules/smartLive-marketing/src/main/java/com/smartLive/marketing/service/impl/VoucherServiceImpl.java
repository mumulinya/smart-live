package com.smartLive.marketing.service.impl;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.ItemActionType;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.marketing.domain.SeckillVoucher;
import com.smartLive.marketing.domain.VO.VoucherVO;
import com.smartLive.marketing.service.ISeckillVoucherService;
import com.smartLive.marketing.until.RedisIdWorker;
import com.smartLive.order.api.DTO.VoucherOrderDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.smartLive.marketing.mapper.VoucherMapper;
import com.smartLive.marketing.domain.Voucher;
import com.smartLive.marketing.service.IVoucherService;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

/**
 * 优惠券Service业务层处理
 *
 * @author 木木林
 * @date 2025-09-21
 */
@Service
@Slf4j
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService
{
    @Autowired
    private VoucherMapper voucherMapper;
    @Autowired
    private RedisService redisService;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RemoteShopService remoteShopService;

    @Autowired
    private RemoteStarService remoteStarService;
    @Autowired
    private RemoteFollowService remoteFollowService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ExecutorService executorService;

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
    public VoucherVO selectVoucherById(Long id)
    {
        Voucher voucher = voucherMapper.selectVoucherById(id);
        if (voucher != null){
            querySeckill(voucher);
            queryVoucherShopMessage(voucher);
//            //判断是否收藏
//            StarDTO starDTO=new StarDTO();
//            starDTO.setSourceType(GlobalBizTypeEnum.VOUCHER.getCode());
//            starDTO.setSourceId(id);
//            Boolean isStar = remoteStarService.isStar(starDTO);
//            voucher.setIsStar(isStar);
//            //判断是否关注
//            FollowDTO followDTO=new FollowDTO();
//            followDTO.setSourceType(GlobalBizTypeEnum.VOUCHER.getCode());
//            followDTO.setSourceId(id);
//            Boolean isFollow = remoteFollowService.isFollowed(followDTO);
//            voucher.setIsFollow(isFollow);
        }
        return convertToVoucherVO(voucher);
    }

    @Override
    public Voucher selectVoucherEntityById(Long id)
    {
        Voucher voucher = voucherMapper.selectVoucherById(id);
        if (voucher != null){
            querySeckill(voucher);
            queryVoucherShopMessage(voucher);
//            //鍒ゆ柇鏄惁鏀惰棌
//            StarDTO starDTO=new StarDTO();
//            starDTO.setSourceType(GlobalBizTypeEnum.VOUCHER.getCode());
//            starDTO.setSourceId(id);
//            Boolean isStar = remoteStarService.isStar(starDTO);
//            voucher.setIsStar(isStar);
//            //鍒ゆ柇鏄惁鍏虫敞
//            FollowDTO followDTO=new FollowDTO();
//            followDTO.setSourceType(GlobalBizTypeEnum.VOUCHER.getCode());
//            followDTO.setSourceId(id);
//            Boolean isFollow = remoteFollowService.isFollowed(followDTO);
//            voucher.setIsFollow(isFollow);
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
    public List<Voucher> selectVoucherEntityList(Voucher voucher)
    {
        List<Voucher> voucherList = voucherMapper.selectVoucherList(voucher);
        voucherList.forEach(v -> {
            querySeckill(v);
        });
        return voucherList;
    }

    @Override
    public List<VoucherVO> selectVoucherList(Voucher voucher)
    {
        List<Voucher> voucherList = selectVoucherEntityList(voucher);
        return convertToVoucherVOList(voucherList);
    }

    /**
     * 查询代金券的秒杀信息
     * @param v
     */
    void querySeckill(Voucher v){
        SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", v.getId()).one();
        if(v.getType()==1&&seckillVoucher!=null){
            v.setStock(seckillVoucher.getStock());
            v.setBeginTime(seckillVoucher.getBeginTime());
            v.setEndTime(seckillVoucher.getEndTime());
        }
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
            boolean b = addSeckillVoucher(voucher);
            if (b){
                sendNewVoucherMessageToMQ(voucher);
                return 1;
            }
            return 0;
        }
        //保存优惠券
        int i = voucherMapper.insertVoucher(voucher);
        if(i>0){
            //发送消息推送动态
            sendNewVoucherMessageToMQ(voucher);
        }
        return i;
    }
    /**
     * 店铺发布新代金券，更新用户动态
     * @param voucher
     */
    public void  sendNewVoucherMessageToMQ(Voucher voucher){
        FeedEventMessage feedEventMessage= FeedEventMessage
                .builder()
                .feedType(FeedTypeEnum.SHOP_FEED.getCode())
                .sourceType(GlobalBizTypeEnum.SHOP.getCode())
                .sourceId(voucher.getShopId())
                .bizType(GlobalBizTypeEnum.VOUCHER.getCode())
                .bizId(voucher.getId())
                .publishTime(DateUtils.getNowDate())
                //动态动作 新品
                .action(ItemActionType.NEW_ITEM.getCode())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                MqConstants.INTERACT_FEED_EXCHANGE_NAME,
                MqConstants.INTERACT_FEED_VOUCHER_ROUTING,
                feedEventMessage);
    }
    public void sendVoucherActionMessageToMQ(Long voucherId,ItemActionType itemActionType){
        FeedEventMessage feedEventMessage= FeedEventMessage
                .builder()
                .feedType(FeedTypeEnum.ITEM_FEED.getCode())
                .sourceType(GlobalBizTypeEnum.VOUCHER.getCode())
                .sourceId(voucherId)
                .bizType(GlobalBizTypeEnum.VOUCHER.getCode())
                .bizId(voucherId)
                .publishTime(DateUtils.getNowDate())
                //动态动作 新品
                .action(itemActionType.getCode())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                MqConstants.INTERACT_FEED_EXCHANGE_NAME,
                MqConstants.INTERACT_FEED_VOUCHER_ROUTING,
                feedEventMessage);
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
                seckillVoucher.setBeginTime(voucher.getBeginTime());
                seckillVoucher.setEndTime(voucher.getEndTime());
                seckillVoucher.setUpdateTime(DateUtils.getNowDate());
                seckillVoucherService.updateById(seckillVoucher);
                //更新redis库存数据
                redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY+voucher.getId(),voucher.getStock());
            }
            //更新es数据
            publish(new String[]{voucher.getId().toString()});
        }
        return i;
    }

    /**
     * 修改优惠券状态
     *
     * @param voucher 优惠券
     * @return 修改结果
     */
    @Override
    public Boolean changeStatus(Voucher voucher) {
        boolean b = updateById(voucher);
        if (b){
            //更新es数据
            publish(new String[]{voucher.getId().toString()});
            if (voucher.getStatus()==1){
                //上架，发送mq消息更新用户动态
                sendVoucherActionMessageToMQ(voucher.getId(), ItemActionType.RESHELF);
            }
        }
        return b;
    }

    /**
     * 优惠券价格下降
     *
     * @param id 优惠券id
     * @return 优惠券价格下降结果
     */
    @Override
    public int priceReduced(Long id) {
        sendVoucherActionMessageToMQ(id,ItemActionType.PRICE_DROP);
        return 1;
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
        int i = voucherMapper.deleteVoucherByIds(ids);
        //删除es数据
        if (i > 0) {
        for (Long id : ids) {
            executorService.submit(()->{
                log.info("线程“{}删除es数据id为：{}", id);
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setId(id);
                contentSyncMessage.setIndexName(EsIndexNameConstants.VOUCHER_INDEX_NAME);
                contentSyncMessage.setType(GlobalBizTypeEnum.VOUCHER.getCode());
                //发起rabbitMq信息删除es数据
//               rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_VOUCHER_DELETE,esInsertRequest);
                MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_VOUCHER_DELETE, contentSyncMessage);
                //发起rabbitmq信息删除milvus数据
//               rabbitTemplate.convertAndSend(MqConstants.MILVUS_EXCHANGE,MqConstants.MILVUS_ROUTING_VOUCHER_DELETE,esInsertRequest);
            });
        }
        }
        return 1;
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
    public Long seckillVoucher(Long voucherId, Long userId) {
        //获取订单id
        Long orderId = redisIdWorker.nextId("order");
        //1.执行lua脚本
        Long result = redisService.executeScript(SECKILL_SCRIPT,
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
                    throw new BusinessException("库存不足");
                case 2:
                    throw new BusinessException("不能重复下单");
                case 3:
                    throw new BusinessException("活动结束");
            }
        }
        //创建订单
        VoucherOrderDTO voucherOrder = new VoucherOrderDTO();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //发送消息
//        rabbitTemplate.convertAndSend(MqConstants.ORDER_EXCHANGE_NAME, MqConstants.ORDER_SECKILL_ROUTING, voucherOrder);
//        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_SECKILL_ROUTING,voucherOrder);
//        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_SECKILL_ROUTING,voucherOrder,MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME, MqConstants.ORDER_DEAD_LETTER_ROUTING,3);
        try {
            executorService.submit(()->{
                log.info("线程{}创建秒杀订单id为：{}", Thread.currentThread().getName(), orderId);
                MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_SECKILL_ROUTING,voucherOrder,MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME, MqConstants.ORDER_DEAD_LETTER_ROUTING,3);
            });
        }catch (Exception e){
            log.error("线程{}创建秒杀订单id为：{}失败", Thread.currentThread().getName(), orderId);
            //恢复库存
            seckillVoucherService.recoverVoucherStock(voucherId);
        }
        //发送延迟消息，检测订单支付状态
//        rabbitTemplate.convertAndSend(MqConstants.ORDER_DELAY_EXCHANGE_NAME, MqConstants.ORDER_DELAY_ROUTING, voucherOrder.getId(), message -> {
//            message.getMessageProperties().setDelay(MqConstants.DELAY_TIME);
//            return message;
//        });
//        MqMessageSendUtils.sendSessionMessage(rabbitTemplate,MqConstants.ORDER_DELAY_EXCHANGE_NAME,MqConstants.ORDER_DELAY_ROUTING,voucherOrder.getId(),(MqConstants.DELAY_TIME));
        //获取事务代理对象
        proxy= (IVoucherService) AopContext.currentProxy();
        //3 返回订单id
        return orderId;
    }


    /**
     * 购买优惠券
     * @param voucherId
     * @return
     */
    @Override
    public Long buyVoucher(Long voucherId, Long userId) {
        //获取订单id
        Long orderId = redisIdWorker.nextId("order");
        VoucherOrderDTO voucherOrder = new VoucherOrderDTO();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //5.发送消息创建订单
//        rabbitTemplate.convertAndSend(MqConstants.ORDER_EXCHANGE_NAME, MqConstants.ORDER_BUY_ROUTING, voucherOrder);
//        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_BUY_ROUTING,voucherOrder);
//        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_BUY_ROUTING,voucherOrder,MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME, MqConstants.ORDER_DEAD_LETTER_ROUTING,3);
        executorService.submit(()->{
            log.info("线程“{}创建普通订单id为：{}", Thread.currentThread().getName(), orderId);
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_BUY_ROUTING,voucherOrder,MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME, MqConstants.ORDER_DEAD_LETTER_ROUTING,3);
        });
//        //发送延迟消息，检测订单支付状态
//        MqMessageSendUtils.sendSessionMessage(rabbitTemplate,MqConstants.ORDER_DELAY_EXCHANGE_NAME,MqConstants.ORDER_DELAY_ROUTING,voucherOrder.getId(),(MqConstants.DELAY_TIME));
//        rabbitTemplate.convertAndSend(MqConstants.ORDER_DELAY_EXCHANGE_NAME, MqConstants.ORDER_DELAY_ROUTING, voucherOrder.getId(), message -> {
//            message.getMessageProperties().setDelay(MqConstants.DELAY_TIME);
//            return message;
//        });
//        save(voucherOrder);
        //6.返回订单id
        return voucherOrder.getId();
    }
    /**
     * 根据店铺查询优惠券列表
     *
     * @param shopId
     * @return
     */
    @Override
    public List<VoucherVO> queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return convertToVoucherVOList(vouchers);
    }

    /**
     * 添加秒杀券
     *
     * @param voucher
     */
    @Override
    public boolean addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        boolean save = save(voucher);
        if(save){
            // 保存秒杀信息
            SeckillVoucher seckillVoucher = new SeckillVoucher();
            seckillVoucher.setVoucherId(voucher.getId());
            seckillVoucher.setStock(voucher.getStock());
            seckillVoucher.setBeginTime(voucher.getBeginTime());
            seckillVoucher.setEndTime(voucher.getEndTime());
            boolean b = seckillVoucherService.save(seckillVoucher);
            if(b){
                //把秒杀库存写入redis
                String key= RedisConstants.SECKILL_STOCK_KEY + voucher.getId();
                redisService.setCacheObject(key, voucher.getStock());
                return true;
            }
            return false;
        }
        return false;
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
            queryVoucherShopMessage(voucher);
        });
        return list;
    }
    /**
     * 查询代金券店铺信息
     * @param voucher
     */
    void queryVoucherShopMessage(Voucher voucher){
        ShopDTO shopDTO = remoteShopService.getShopById(voucher.getShopId());
        if(shopDTO != null){
            voucher.setShopName(shopDTO.getName());
            voucher.setTypeId(shopDTO.getTypeId());
            voucher.setShopImages(shopDTO.getImages());
        }
    }

    private VoucherVO convertToVoucherVO(Voucher voucher) {
        if (voucher == null) {
            return null;
        }
        VoucherVO voucherVO = new VoucherVO();
        BeanUtils.copyProperties(voucher, voucherVO);
        return voucherVO;
    }

    private List<VoucherVO> convertToVoucherVOList(List<Voucher> voucherList) {
        if (voucherList == null || voucherList.isEmpty()) {
            return new ArrayList<>();
        }
        List<VoucherVO> voList = new ArrayList<>(voucherList.size());
        for (Voucher voucher : voucherList) {
            voList.add(convertToVoucherVO(voucher));
        }
        return voList;
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
            ShopDTO shop = remoteShopService.getShopByShopName(voucher.getShopName());
            if(shop != null){
                shopId = shop.getId();
            }
        }
        return query().eq(shopId != null,"shop_id", shopId).eq("type", 1).list();
    }

    /**
     * 获取优惠券总数
     *
     * @return 优惠券总数
     */
    @Override
    public Integer getCouponTotal() {
        return query().eq("type", 0).count().intValue();
    }

    /**
     * 获取优惠券列表
     *
     * @param sourceIdList 优惠券id列表
     * @return 优惠券列表
     */
    @Override
    public List<Voucher> getVoucherListByIds(List<Long> sourceIdList) {
        List<Voucher> voucherList = query().in("id", sourceIdList).list();
        voucherList.forEach(voucher -> {
            if(voucher!= null){
                if(voucher.getType() == 1){
                    querySeckill(voucher);
                }
                queryVoucherShopMessage(voucher);
            }
        });
        log.info("查询优惠券列表：{}", voucherList);
        return voucherList;
    }

    /**
     * 获取优惠券
     *
     * @param id 优惠券id
     * @return 优惠券
     */
    @Override
    public VoucherVO getVoucherById(Long id) {
        Voucher voucher = voucherMapper.selectVoucherById(id);
        if (voucher != null){
            //查询是否是秒杀代金券
            querySeckill(voucher);
            //判断是否收藏
            StarDTO starDTO=new StarDTO();
            starDTO.setSourceType(GlobalBizTypeEnum.VOUCHER.getCode());
            starDTO.setSourceId(id);
            Boolean isStar = remoteStarService.isStar(starDTO);
            voucher.setIsStar(isStar);
            //判断是否关注
            FollowDTO followDTO=new FollowDTO();
            followDTO.setSourceType(GlobalBizTypeEnum.VOUCHER.getCode());
            followDTO.setSourceId(id);
            Boolean isFollow = remoteFollowService.isFollowed(followDTO);
            voucher.setIsFollow(isFollow);
        }
        return convertToVoucherVO(voucher);
    }

    /**
     * 添加库存
     *
     * @param id 优惠券id
     * @return 添加结果
     */
    @Override
    public int addStock(Long id) {
        log.info("发送mq消息，更新用户动态");
        //发送消息，更新用户动态
       sendVoucherActionMessageToMQ(id, ItemActionType.RESTOCK);
        return 1;
    }

    /**
     * 全部发布
     *
     * @return 全部发布结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE; // 每页50条
        while (true) {
            // 分页查询
            List<Voucher> vouchers = query()
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (vouchers.isEmpty()) {
                break;
            }
            // 定义结果 Map，默认为空
            Map<Long, ShopDTO> ShopDTOMap = Collections.emptyMap();
            //获取店铺id列表
            List<Long> shopIds = vouchers.stream()
                    .map(Voucher::getShopId)
                    .filter(Objects::nonNull) // 防止有 null 的 userId 导致报错
                    .distinct()               // 去重，避免重复查询同一个 ID
                    .collect(Collectors.toList());
            // 2. 只有当 ID 列表不为空时才发起远程调用，节省资源
            if (!shopIds.isEmpty()) {
                // 批量查询用户信息
                List<ShopDTO> shopDTOList = remoteShopService.getShopList(shopIds);
                // 3. 安全获取 List 数据 (防止远程调用返回 null 或者 data 为 null)
                // 4. 将 List<User> 转换为 Map<Long, User>
              if(shopDTOList != null && shopDTOList.size() > 0){
                  ShopDTOMap = shopDTOList.stream().collect(Collectors.toMap(
                          ShopDTO::getId,               // Key: 用户 ID
                          Function.identity(),       // Value: User 对象本身
                          (v1, v2) -> v1             // MergeFunction: 如果远程服务返回了重复 ID 的数据，取第一个，防止报错
                  ));              }
            }
            Map<Long, ShopDTO> finalShopDTOMap = ShopDTOMap;

            //查询秒杀优惠券信息列表
            Map<Long, SeckillVoucher> SeckillVoucherMap = Collections.emptyMap();
            List<Long> voucherIds = vouchers.stream()
                    .map(Voucher::getId)
                    .filter(Objects::nonNull) // 防止有 null 的 userId 导致报错
                    .distinct()               // 去重，避免重复查询同一个 ID
                    .collect(Collectors.toList());
            // 2. 只有当 ID 列表不为空时才发起远程调用，节省资源
            if (!voucherIds.isEmpty()) {
                // 批量查询用户信息
                List<SeckillVoucher> seckillVoucherList =seckillVoucherService.listSeckillVoucher(voucherIds);
                // 4. 将 List<User> 转换为 Map<Long, User>
                SeckillVoucherMap = seckillVoucherList.stream().collect(Collectors.toMap(
                        SeckillVoucher::getVoucherId,               // Key: 用户 ID
                        Function.identity(),       // Value: User 对象本身
                        (v1, v2) -> v1             // MergeFunction: 如果远程服务返回了重复 ID 的数据，取第一个，防止报错
                ));
            }
            Map<Long, SeckillVoucher> finalSeckillVoucherMap = SeckillVoucherMap;
            int finalPage = page;
            executorService.submit(()->{
                log.info("线程：{}开始发布的优惠券{}页", Thread.currentThread().getName(), finalPage);
                vouchers.forEach(voucher -> {
                    SeckillVoucher seckillVoucher = finalSeckillVoucherMap.get(voucher.getId());
                    if (seckillVoucher != null) {
                        voucher.setBeginTime(seckillVoucher.getBeginTime());
                        voucher.setEndTime(seckillVoucher.getEndTime());
                        voucher.setStock(seckillVoucher.getStock());
                    }
                    ShopDTO shopDTO = finalShopDTOMap.get(voucher.getShopId());
                    if(shopDTO != null){
                        voucher.setShopName(shopDTO.getName());
                        voucher.setTypeId(shopDTO.getTypeId());
                    }
//                   querySeckill(voucher);
//                   queryVoucherShopMessage(voucher);
                });
                // 创建请求并发送
                ContentBatchSyncMessage request = new ContentBatchSyncMessage();
                request.setIndexName(EsIndexNameConstants.VOUCHER_INDEX_NAME);
                request.setData(vouchers);
                request.setType(GlobalBizTypeEnum.VOUCHER.getCode());
                // 发送rabbitmq消息数据插入es
//               rabbitTemplate.convertAndSend(
//                       MqConstants.ES_EXCHANGE,
//                       MqConstants.ES_ROUTING_VOUCHER_BATCH_INSERT,
//                       request
//               );
                MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_VOUCHER_BATCH_INSERT, request);
                //发送rabbitmq消息数据插入Milvus
               rabbitTemplate.convertAndSend(
                       MqConstants.MILVUS_EXCHANGE,
                       MqConstants.MILVUS_ROUTING_VOUCHER_BATCH_INSERT,
                       request
               );
                log.info("发送第 {} 页，{} 条数据", finalPage, vouchers.size());
            });
            page++;
        }
        return "数据发布完成";
    }


    /**
     * 发布
     *
     * @param
     * @return 发布结果
     */
    @Override
    public String publish(String[] ids) {
        for (String id : ids) {
            executorService.submit(()->{
                log.info("线程：{}开始发布id为{}的优惠券", Thread.currentThread().getName(), id);
                Voucher voucher = getById(Long.parseLong(id));
                if (voucher== null){
                    log.info("优惠券不存在");
                    return;
                }
                querySeckill(voucher);
                queryVoucherShopMessage(voucher);
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setIndexName(EsIndexNameConstants.VOUCHER_INDEX_NAME);
                contentSyncMessage.setData(voucher);
                contentSyncMessage.setId(voucher.getId());
                contentSyncMessage.setType(GlobalBizTypeEnum.VOUCHER.getCode());
                log.info("发送的优惠券信息为{}", voucher);
                //发送rabbitmq消息数据插入es
//               rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_VOUCHER_INSERT, esInsertRequest);
                MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_VOUCHER_INSERT, contentSyncMessage);
                //发送rabbitmq消息数据插入Milvus
               rabbitTemplate.convertAndSend(MqConstants.MILVUS_EXCHANGE, MqConstants.MILVUS_ROUTING_VOUCHER_INSERT, contentSyncMessage);
            });
        }
        return "发布成功";
    }

    /**
     * 批量更新评价数
     *
     * @param updateMap 批量更新评价数
     * @return 批量更新评价数结果
     */
    @Override
    public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 建议：如果数量特别大(超过500)，建议分批，防止 SQL 语句超长报错
        // 如果你确信每 30秒 的点赞更新量不会导致 SQL 超过 4MB，可以直接调 baseMapper
        if (updateMap.size() > 500) {
            // 分批逻辑 (每500条提交一次)
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateReviewCountBatch(batchMap);
            }
        } else {
            // 数量少直接执行
            baseMapper.updateReviewCountBatch(updateMap);
        }
        return true;
    }
    /**
     * 批量更新商铺收藏数
     *
     * @param updateMap 商铺id和收藏数
     * @return 更新结果
     */
    @Override
    public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 建议：如果数量特别大(超过500)，建议分批，防止 SQL 语句超长报错
        // 如果你确信每 30秒 的点赞更新量不会导致 SQL 超过 4MB，可以直接调 baseMapper
        if (updateMap.size() > 500) {
            // 分批逻辑 (每500条提交一次)
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateStarCountBatch(batchMap);
            }
        } else {
            // 数量少直接执行
            baseMapper.updateStarCountBatch(updateMap);
        }
        return true;
    }

    /**
     * 获取代金券收藏数
     *
     * @param sourceId 优惠券id
     * @return 收藏数
     */
    @Override
    public Integer getVoucherStarCount(Long sourceId) {
        Voucher voucher = lambdaQuery()
                .select(Voucher::getStars)
                .eq(Voucher::getId, sourceId)
                .one();
        return voucher != null ? voucher.getStars() : 0;
    }
}
