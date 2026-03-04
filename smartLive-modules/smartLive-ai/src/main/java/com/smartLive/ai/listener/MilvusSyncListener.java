package com.smartLive.ai.listener;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;

import com.rabbitmq.client.Channel;
import com.smartLive.ai.strategy.factory.MilvusSyncFactory;
import com.smartLive.ai.strategy.milvus.MilvusSyncStrategy;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.util.DigestUtils;

@Component
@Slf4j
public class MilvusSyncListener {

    @Autowired
    private MilvusSyncFactory milvusSyncFactory;

    @Autowired
    private RedisService redisService;

    /**
     * 监听单条数据插入或更新请求，同步到 Milvus 向量数据库
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.MILVUS_INSERT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.MILVUS_EXCHANGE),
                    key = SearchMqConstants.MILVUS_ROUTING_INSERT)
    })
    public void handleSingleInsert(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] Milvus同步消息缺失 messageId，拒绝消费. dataId={}", request.getId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "insert:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.MILVUS_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            MilvusSyncStrategy strategy = milvusSyncFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("Milvus single insert failed, strategy not found for type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean success = strategy.insertOrUpdate(request.getId().toString(), request.getData());
            log.info("Milvus single insert result: {}, type={}", success, request.getType());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] Milvus单条同步异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("Milvus单条同步异常，触发本地重试", e);
        }
    }

    /**
     * 监听批量数据插入请求，批量同步到 Milvus 向量数据库
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.MILVUS_BATCH_INSERT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.MILVUS_EXCHANGE),
                    key = SearchMqConstants.MILVUS_ROUTING_BATCH_INSERT)
    })
    public void handleBatchInsert(ContentBatchSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getData() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] Milvus批量同步消息缺失 messageId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "batch:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.MILVUS_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            MilvusSyncStrategy strategy = milvusSyncFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("Milvus batch insert failed, strategy not found for type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            @SuppressWarnings("unchecked")
            boolean success = strategy.batchInsert((List<Object>) request.getData());
            log.info("Milvus batch insert result: {}, type={}", success, request.getType());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] Milvus批量同步异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("Milvus批量同步异常，触发本地重试", e);
        }
    }

    /**
     * 监听数据删除请求，从 Milvus 向量数据库中删除对应的数据
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.MILVUS_DELETE_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.MILVUS_EXCHANGE),
                    key = SearchMqConstants.MILVUS_ROUTING_DELETE)
    })
    public void handleDelete(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] Milvus删除消息缺失 messageId，拒绝消费. dataId={}", request.getId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "delete:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.MILVUS_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            MilvusSyncStrategy strategy = milvusSyncFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("Milvus delete failed, strategy not found for type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean success = strategy.delete(request.getId().toString());
            log.info("Milvus delete result: {}", success);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] Milvus删除处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("Milvus删除处理异常，触发本地重试", e);
        }
    }

    private boolean isDefaultStrategy(MilvusSyncStrategy strategy) {
        return strategy == null || Integer.valueOf(-1).equals(strategy.getType());
    }

    /**
     * 监听由于 Milvus 同步异常等原因进入的死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = SearchMqConstants.SEARCH_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
            key = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING
    ))
    public void handleMilvusDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("milvus dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
