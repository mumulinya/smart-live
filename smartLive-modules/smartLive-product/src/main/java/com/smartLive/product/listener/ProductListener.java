package com.smartLive.product.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.core.constant.mq.ProductMqConstants;
import com.smartLive.common.rabbitmq.domain.StockDeductMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.product.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class ProductListener {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisService redisService;
    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 批量监听扣减库存的MQ消息，聚合后一次性更新MySQL
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = ProductMqConstants.PRODUCT_STOCK_DEDUCT_QUEUE, declare = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = ProductMqConstants.PRODUCT_DLX_EXCHANGE),
                                    @Argument(name = "x-dead-letter-routing-key", value = ProductMqConstants.PRODUCT_DLQ_ROUTING_KEY)
                            }
                    ),
                    exchange = @Exchange(name = ProductMqConstants.PRODUCT_STOCK_EXCHANGE, type = ExchangeTypes.DIRECT),
                    key = ProductMqConstants.PRODUCT_STOCK_DEDUCT_ROUTING_KEY
            ),
            containerFactory = "rabbitListenerContainerFactory" 
            // 依赖于 bootstrap.yml 中的 batch-enabled: true
    )
    public void batchDeductStock(List<Message> messages, Channel channel) throws IOException { // 拿到了一批消息
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        // 1. 获取 delivery tag 进行批量确权
        long deliveryTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();

        log.info("【商品模块】收到批量预扣库存消息，批次大小: {}", messages.size());

        Map<Long, Integer> deductMap = new HashMap<>(); // [商品ID, 本批次扣减总数]
        Map<Long, List<Long>> productHoldOrderMap = new HashMap<>(); // [商品ID, [相关联的OrderId]]

        // 收集获取锁成功记录防重键，如果出错好一起回滚锁
        Map<String, StockDeductMessage> idempotentKeyMap = new HashMap<>();

        try {
            for (Message msg : messages) {
                String messageId = msg.getMessageProperties().getMessageId();
                if (messageId == null) {
                    continue; // 无头垃圾跳过
                }

                String idempotentKey = RedisMqIdempotentConstants.PRODUCT_PREFIX + "stock:deduct:" + messageId;
                
                // 【幂等】 检查本批次中的每一条
                if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                    log.info("[MQ幂等] 本批次含有重复的扣库消息，跳过. msgId={}", messageId);
                    continue; 
                }

                StockDeductMessage body = objectMapper.readValue(msg.getBody(), StockDeductMessage.class);
                idempotentKeyMap.put(idempotentKey, body);

                // 累加扣减数
                deductMap.merge(body.getProductId(), body.getCount(), Integer::sum);
                // 记录商品分别对应哪些订单, 以备退回
                productHoldOrderMap.computeIfAbsent(body.getProductId(), k -> new java.util.ArrayList<>()).add(body.getOrderId());
            }

            if (deductMap.isEmpty()) {
                channel.basicAck(deliveryTag, true); // 全是重复的，不处理，批量确认 
                return;
            }

            // 2. 批量查数据库进行扣库 （由于 MyBatis 支持 foreach 写法不够好，我们在循环中单个 Update）
            // 我们写成了 WHERE id = #{id} AND stock >= #{count} 的 CAS 操作
            for (Map.Entry<Long, Integer> entry : deductMap.entrySet()) {
                Long productId = entry.getKey();
                Integer totalCount = entry.getValue();

                int rows = productMapper.deductStock(productId, totalCount);

                if (rows == 0) {
                    // 扣减失败，库存已经不足了！（超扣红线触发）
                    log.error("【严重】批量扣库失败（超扣底线触发），productId={}, 计划扣减量={}！进行回滚订单通知补偿...", productId, totalCount);
                    // 找出这次批次里这件商品关联的所有订单，发送回滚 MQ 到订单模块，直接把单据关掉/退款
                    List<Long> failedOrderIds = productHoldOrderMap.get(productId);
                    for (Long failedOrderId : failedOrderIds) {
                        log.warn("发送恢复订单请求。 订单：{}", failedOrderId);
                        mqMessageSendUtils.sendMqMessage(
                                OrderMqConstants.ORDER_CANCEL_EXCHANGE,
                                OrderMqConstants.ORDER_CANCEL_ROUTING_KEY,
                                failedOrderId,
                                0
                        );
                    }
                } else {
                    log.info("批量扣库成功！productId={}, 共扣掉了一次性 {} 个库存！", productId, totalCount);
                }
            }

            channel.basicAck(deliveryTag, true);
        } catch (Exception e) {
            log.error("[MQ幂等] 批量预扣库存出现致命的内部异常！释放拿到的幂等锁并触发本地重连", e);
            idempotentKeyMap.keySet().forEach(key -> redisService.deleteObject(key));
            throw new RuntimeException("批量预扣库存出现代码异常，触发本地重试！", e);
        }
    }


    /**
     * 监听处理商品各种原因导致的死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = ProductMqConstants.PRODUCT_DLQ_QUEUE, durable = "true"),
            exchange = @Exchange(value = ProductMqConstants.PRODUCT_DLX_EXCHANGE),
            key = ProductMqConstants.PRODUCT_DLQ_ROUTING_KEY
    ))
    public void handleDeadLetter(Message message, Channel channel, @org.springframework.messaging.handler.annotation.Header(org.springframework.amqp.support.AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("商品死信队列收到无法处理的消息体，可能是重试耗尽");
        channel.basicAck(deliveryTag, false);
    }
}
