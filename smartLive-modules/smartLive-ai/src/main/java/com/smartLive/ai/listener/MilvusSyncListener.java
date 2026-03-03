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
     * Milvus single insert.
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
    public void handleSingleInsert(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }
        String bizKey = request.getType() + ":" + request.getId() + ":insert";
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
            log.error("[MQ幂等] Milvus single insert 失败，key={}", idempotentKey, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Milvus batch insert.
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
    public void handleBatchInsert(ContentBatchSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (request == null || request.getData() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }
        String dataHash = DigestUtils.md5DigestAsHex(request.getData().toString().getBytes(StandardCharsets.UTF_8));
        String bizKey = request.getType() + ":batch:" + dataHash;
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
            log.error("[MQ幂等] Milvus batch insert 失败，key={}", idempotentKey, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Milvus delete.
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
    public void handleDelete(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }
        String bizKey = request.getType() + ":" + request.getId() + ":delete";
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
            log.error("[MQ幂等] Milvus delete 失败，key={}", idempotentKey, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private boolean isDefaultStrategy(MilvusSyncStrategy strategy) {
        return strategy == null || Integer.valueOf(-1).equals(strategy.getType());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = SearchMqConstants.SEARCH_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
            key = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING
    ))
    public void handleMilvusDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("milvus dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
