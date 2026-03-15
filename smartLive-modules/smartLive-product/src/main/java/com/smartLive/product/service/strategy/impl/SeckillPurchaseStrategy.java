package com.smartLive.product.service.strategy.impl;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import com.smartLive.common.core.enums.product.ProductEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.order.api.DTO.OrderDTO;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.strategy.PurchaseStrategy;
import com.smartLive.product.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * 秒杀商品抢购策略
 * 核心逻辑：
 * 1. 使用 Lua 脚本保证检查库存、扣减预热库存及判重下单的原子性。
 * 2. 只有脚本返回成功后，才通过消息队列异步发起真实扣减与订单入库。
 * 
 * @author smartLive
 * @date 2026-02-18
 */
@Slf4j
@Component("SeckillPurchaseStrategy")
public class SeckillPurchaseStrategy implements PurchaseStrategy {

    @Autowired
    private RedisIdWorker redisIdWorker;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private RedisService redisService;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 执行秒杀抢购逻辑
     * 
     * 流程：
     * 1. 生成全局唯一订单 ID 项。
     * 2. 执行 seckill.lua 脚本（原子化校验：库存 > 0 且 用户未重复下单）。
     * 3. 根据脚本返回值（0:成功, 1:无库存, 2:重复下单, 3:活动结束）决定是否抛出异常。
     * 4. 预设 Redis 订单状态占位符。
     * 5. 发送异步 MQ 消息至订单微服务完成最终扣减与落库。
     *
     * @param userId  用户 ID
     * @param product 抢购商品实体
     * @return 订单 ID
     */
    @Override
    public Long purchase(Long userId, Product product) {
        // 1. 获取订单由 Redis 生成的全局 ID
        Long orderId = redisIdWorker.nextId("order");

        // 2. 执行 Lua 脚本进行预检与预扣（保证原子性，防止超卖）
        Long result = redisService.executeScript(SECKILL_SCRIPT,
                Collections.emptyList(),
                product.getId().toString(),
                userId.toString(),
                String.valueOf(orderId));
        int r = result.intValue();

        // 3. 处理脚本执行结果
        if (r != 0) {
            // 脚本返回非 0 代表校验不通过
            switch (r) {
                case 1:
                    throw new BusinessException("库存不足，下次早点来哦");
                case 2:
                    throw new BusinessException("您已经抢购过了，请勿重复下单");
                case 3:
                    throw new BusinessException("秒杀活动已结束");
            }
        }

        // 4. 创建订单传输对象
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(orderId);
        orderDTO.setUserId(userId);
        orderDTO.setSourceType(resolveOrderSourceType(product));
        orderDTO.setSourceId(product.getId());
        orderDTO.setPayAmount(product.getPrice());
        orderDTO.setShopId(product.getShopId());

        // 5. 在 Redis 记录临时创建状态，防止前端查不到订单
        redisService.setCacheObject("order:status:" + orderId, "CREATING", 60L, java.util.concurrent.TimeUnit.SECONDS);

        // 6. 异步发送 MQ 消息，进入订单创建流程
        try {
            executorService.submit(() -> {
                log.info("线程 {} 成功接单，正在为用户 {} 生成秒杀订单: {}", Thread.currentThread().getName(), userId, orderId);
                mqMessageSendUtils.sendMqMessage(
                        OrderMqConstants.ORDER_DIRECT_EXCHANGE,
                        OrderMqConstants.ORDER_SECKILL_ROUTING_KEY,
                        orderDTO,
                        3);
            });
        } catch (Exception e) {
            log.error("秒杀订单发送 MQ 失败，订单号: {}, 异常原因: {}", orderId, e.getMessage());
            // TODO: 未来可在此处实现 Redis 库存补偿逻辑
        }
        return orderId;
    }
    private Integer resolveOrderSourceType(Product product) {
        Integer category = product == null ? null : product.getCategory();
        if (ProductEnum.VOUCHER.getCode().equals(category) || ProductEnum.SET_MEAL.getCode().equals(category)) {
            return category;
        }
        throw new BusinessException("unsupported product category");
    }
}
