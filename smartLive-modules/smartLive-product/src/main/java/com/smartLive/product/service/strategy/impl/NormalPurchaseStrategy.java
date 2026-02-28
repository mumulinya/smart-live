package com.smartLive.product.service.strategy.impl;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.order.api.DTO.VoucherOrderDTO;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.strategy.PurchaseStrategy;
import com.smartLive.product.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * 普通商品购买策略
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@Slf4j
@Component("NormalPurchaseStrategy")
public class NormalPurchaseStrategy implements PurchaseStrategy {

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private com.smartLive.common.redis.service.RedisService redisService;
    
    @Override
    public Long purchase(Long userId, Product product) {
        // 获取订单id
        Long orderId = redisIdWorker.nextId("order");
        VoucherOrderDTO voucherOrder = new VoucherOrderDTO();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); 
        voucherOrder.setSourceId(product.getId());

        // Redis 占位: 告诉查询接口 MQ 还在路上
        redisService.setCacheObject("order:status:" + orderId, "CREATING", 60L, java.util.concurrent.TimeUnit.SECONDS);

        // 发送消息创建订单
        executorService.submit(() -> {
            log.info("线程{}创建普通订单id为：{}", Thread.currentThread().getName(), orderId);
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                    OrderMqConstants.ORDER_EXCHANGE_NAME,
                    OrderMqConstants.ORDER_BUY_ROUTING,
                    voucherOrder,
                    OrderMqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME,
                    OrderMqConstants.ORDER_DEAD_LETTER_ROUTING,
                    3);
        });

        return orderId;
    }
}
