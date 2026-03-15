package com.smartLive.product.service.strategy.impl;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import com.smartLive.common.core.enums.product.ProductEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.order.api.DTO.OrderDTO;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.strategy.PurchaseStrategy;
import com.smartLive.product.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * 普通商品购买策略
 * 处理无需高并发抢购逻辑的常规商品下单流程
 * 
 * @author smartLive
 * @date 2026-02-18
 */
@Slf4j
@Component("NormalPurchaseStrategy")
public class NormalPurchaseStrategy implements PurchaseStrategy {

   @Autowired
    private RedisIdWorker redisIdWorker;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private com.smartLive.common.redis.service.RedisService redisService;
    
    /**
     * 执行购买逻辑
     * 1. 生成基于 Redis 的全局唯一订单 ID
     * 2. 封装 OrderDTO 订单基础信息
     * 3. 在 Redis 中预设订单状态为 "CREATING"，解决 MQ 延迟带来的查询空白
     * 4. 异步发送 MQ 消息至订单微服务完成入库及库存扣减
     *
     * @param userId  用户 ID
     * @param product 商品实体
     * @return 预生成的订单 ID
     */
    @Override
    public Long purchase(Long userId, Product product) {
        // 1. 获取全局唯一订单 ID
        Long orderId = redisIdWorker.nextId("order");
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(orderId);
        orderDTO.setUserId(userId);
        orderDTO.setSourceType(resolveOrderSourceType(product));
        orderDTO.setSourceId(product.getId());
        orderDTO.setPayAmount(product.getPrice());
        orderDTO.setShopId(product.getShopId());

        // 2. Redis 占位: 告知查询接口该订单由于 MQ 延迟仍在处理中
        redisService.setCacheObject("order:status:" + orderId, "CREATING", 60L, java.util.concurrent.TimeUnit.SECONDS);

        // 3. 异步发送异步消息，解耦订单创建压力
        executorService.submit(() -> {
            log.info("线程 {} 正在为用户 {} 创建普通订单，订单号: {}", Thread.currentThread().getName(), userId, orderId);
            mqMessageSendUtils.sendMqMessage(
                    OrderMqConstants.ORDER_DIRECT_EXCHANGE,
                    OrderMqConstants.ORDER_BUY_ROUTING_KEY,
                    orderDTO,
                    3);
        });

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
