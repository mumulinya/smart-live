package com.smartLive.marketing.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.domain.EsBatchInsertRequest;
import com.smartLive.common.core.domain.EsInsertRequest;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.rabbitMq.MqMessageSendUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.domain.SeckillVoucher;
import com.smartLive.marketing.service.ISeckillVoucherService;
import com.smartLive.marketing.until.RedisIdWorker;
import com.smartLive.order.api.dto.VoucherOrderDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.smartLive.marketing.mapper.VoucherMapper;
import com.smartLive.marketing.domain.Voucher;
import com.smartLive.marketing.service.IVoucherService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 优惠券Service业务层处理
 *
 * @author ruoyi
 * @date 2025-09-21
 */
@Service
@Slf4j
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService
{
    @Autowired
    private VoucherMapper voucherMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RemoteShopService remoteShopService;
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
    public Voucher selectVoucherById(Long id)
    {
        Voucher voucher = voucherMapper.selectVoucherById(id);
        if (voucher != null){
            querySeckill(voucher);
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
    public List<Voucher> selectVoucherList(Voucher voucher)
    {
        List<Voucher> voucherList = voucherMapper.selectVoucherList(voucher);
        voucherList.forEach(v -> {
            querySeckill(v);
        });
        return voucherList;
    }

    /**
     * 查询代金券的秒杀信息
     * @param v
     */
    void querySeckill(Voucher v){
        SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", v.getId()).one();
        if(v.getType()==1){
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
            addSeckillVoucher(voucher);
            return 1;
        }
        //保存优惠券
        int i = voucherMapper.insertVoucher(voucher);
        return i;
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
            }
            seckillVoucher.setBeginTime(voucher.getBeginTime());
            seckillVoucher.setEndTime(voucher.getEndTime());
            seckillVoucher.setUpdateTime(DateUtils.getNowDate());
            seckillVoucherService.updateById(seckillVoucher);
            //更新es数据
            publish(new String[]{voucher.getId().toString()});
        }
        return i;
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
        //        int i = userMapper.deleteShopByIds(ids);
//        //删除es数据
//        if (i > 0) {
        for (Long id : ids) {
            executorService.submit(()->{
                log.info("线程“{}删除es数据id为：{}", id);
                EsInsertRequest esInsertRequest = new EsInsertRequest();
                esInsertRequest.setId(id);
                esInsertRequest.setIndexName(EsIndexNameConstants.VOUCHER_INDEX_NAME);
                esInsertRequest.setDataType(EsDataTypeConstants.VOUCHER);
                //发起rabbitMq信息删除es数据
//               rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_VOUCHER_DELETE,esInsertRequest);
                MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_VOUCHER_DELETE, esInsertRequest);
                //发起rabbitmq信息删除milvus数据
//               rabbitTemplate.convertAndSend(MqConstants.MILVUS_EXCHANGE,MqConstants.MILVUS_ROUTING_VOUCHER_DELETE,esInsertRequest);
            });
        }
//        }
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
        //创建订单
        VoucherOrderDTO voucherOrder = new VoucherOrderDTO();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //发送消息
//        rabbitTemplate.convertAndSend(MqConstants.ORDER_EXCHANGE_NAME, MqConstants.ORDER_SECKILL_ROUTING, voucherOrder);
//        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_SECKILL_ROUTING,voucherOrder);
//        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_SECKILL_ROUTING,voucherOrder,MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME, MqConstants.ORDER_DEAD_LETTER_ROUTING,3);
        executorService.submit(()->{
            log.info("线程{}创建秒杀订单id为：{}", Thread.currentThread().getName(), orderId);
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME,MqConstants.ORDER_SECKILL_ROUTING,voucherOrder,MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME, MqConstants.ORDER_DEAD_LETTER_ROUTING,3);
        });
        //发送延迟消息，检测订单支付状态
//        rabbitTemplate.convertAndSend(MqConstants.ORDER_DELAY_EXCHANGE_NAME, MqConstants.ORDER_DELAY_ROUTING, voucherOrder.getId(), message -> {
//            message.getMessageProperties().setDelay(MqConstants.DELAY_TIME);
//            return message;
//        });
//        MqMessageSendUtils.sendSessionMessage(rabbitTemplate,MqConstants.ORDER_DELAY_EXCHANGE_NAME,MqConstants.ORDER_DELAY_ROUTING,voucherOrder.getId(),(MqConstants.DELAY_TIME));
        //获取事务代理对象
        proxy= (IVoucherService) AopContext.currentProxy();
        //3 返回订单id
        return Result.ok(orderId);
    }


    /**
     * 购买优惠券
     * @param voucherId
     * @return
     */
    @Override
    public Result buyVoucher(Long voucherId, Long userId) {
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
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ORDER_EXCHANGE_NAME+1,MqConstants.ORDER_BUY_ROUTING,voucherOrder,MqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME, MqConstants.ORDER_DEAD_LETTER_ROUTING,3);
        });
//        //发送延迟消息，检测订单支付状态
//        MqMessageSendUtils.sendSessionMessage(rabbitTemplate,MqConstants.ORDER_DELAY_EXCHANGE_NAME,MqConstants.ORDER_DELAY_ROUTING,voucherOrder.getId(),(MqConstants.DELAY_TIME));
//        rabbitTemplate.convertAndSend(MqConstants.ORDER_DELAY_EXCHANGE_NAME, MqConstants.ORDER_DELAY_ROUTING, voucherOrder.getId(), message -> {
//            message.getMessageProperties().setDelay(MqConstants.DELAY_TIME);
//            return message;
//        });
//        save(voucherOrder);
        //6.返回订单id
        return Result.ok(voucherOrder.getId());
    }
    /**
     * 根据店铺查询优惠券列表
     *
     * @param shopId
     * @return
     */
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    /**
     * 添加秒杀券
     *
     * @param voucher
     */
    @Override
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //把秒杀库存写入redis
        String key= RedisConstants.SECKILL_STOCK_KEY + voucher.getId();
        stringRedisTemplate.opsForValue().set(key, voucher.getStock().toString());
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
        ShopDTO shopDTO = remoteShopService.getShopById(voucher.getShopId()).getData();
        if(shopDTO != null){
            voucher.setShopName(shopDTO.getName());
            voucher.setTypeId(shopDTO.getTypeId());
        }
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
            R<ShopDTO> shop = remoteShopService.getShopByShopName(voucher.getShopName());
            shopId = shop.getData().getId();
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
                R<List<ShopDTO>> response = remoteShopService.getShopList(shopIds);
                // 3. 安全获取 List 数据 (防止远程调用返回 null 或者 data 为 null)
                List<ShopDTO> userList = (response != null && response.getData() != null)
                        ? response.getData()
                        : Collections.emptyList();
                // 4. 将 List<User> 转换为 Map<Long, User>
                ShopDTOMap = userList.stream().collect(Collectors.toMap(
                        ShopDTO::getId,               // Key: 用户 ID
                        Function.identity(),       // Value: User 对象本身
                        (v1, v2) -> v1             // MergeFunction: 如果远程服务返回了重复 ID 的数据，取第一个，防止报错
                ));
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
                EsBatchInsertRequest request = new EsBatchInsertRequest();
                request.setIndexName(EsIndexNameConstants.VOUCHER_INDEX_NAME);
                request.setData(vouchers);
                request.setDataType(EsDataTypeConstants.VOUCHER);
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
                EsInsertRequest esInsertRequest = new EsInsertRequest();
                esInsertRequest.setIndexName(EsIndexNameConstants.VOUCHER_INDEX_NAME);
                esInsertRequest.setData(voucher);
                esInsertRequest.setId(voucher.getId());
                esInsertRequest.setDataType(EsDataTypeConstants.VOUCHER);
                log.info("发送的优惠券信息为{}", voucher);
                //发送rabbitmq消息数据插入es
//               rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_VOUCHER_INSERT, esInsertRequest);
                MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_VOUCHER_INSERT, esInsertRequest);
                //发送rabbitmq消息数据插入Milvus
               rabbitTemplate.convertAndSend(MqConstants.MILVUS_EXCHANGE, MqConstants.MILVUS_ROUTING_VOUCHER_INSERT, esInsertRequest);
            });
        }
        return "发布成功";
    }
}