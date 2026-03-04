package com.smartLive.search.listener;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;

import com.rabbitmq.client.Channel;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.search.strategy.esSync.EsSyncStrategy;
import com.smartLive.search.strategy.factory.EsSyncStrategyFactory;
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
public class EsSyncListener {

    @Autowired
    private EsSyncStrategyFactory esSyncStrategyFactory;

    @Autowired
    private RedisService redisService;

    /**
     * 监听单条数据插入或更新请求，同步到 ES 搜索引擎
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_INSERT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_EXCHANGE),
                    key = SearchMqConstants.ES_ROUTING_INSERT)
    })
    public void handleSingleInsert(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] ES同步消息缺失 messageId，拒绝消费. dataId={}", request.getId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "insert:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES single insert failed, strategy not found for type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean success = strategy.insertOrUpdate(request.getIndexName(), request.getId().toString(), request.getData());
            log.info("ES single insert result: {}, index: {}, id: {}", success, request.getIndexName(), request.getId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] ES单条同步异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES单条同步异常，触发本地重试", e);
        }
    }

    /**
     * 监听批量数据插入请求，批量同步到 ES 搜索引擎
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_BATCH_INSERT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_EXCHANGE),
                    key = SearchMqConstants.ES_ROUTING_BATCH_INSERT)
    })
    public void handleBatchInsert(ContentBatchSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getData() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] ES批量同步消息缺失 messageId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "batch:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES batch insert failed, strategy not found for type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            @SuppressWarnings("unchecked")
            boolean success = strategy.batchInsert(request.getIndexName(), (List<Object>) request.getData());
            log.info("ES batch insert result: {}, index: {}", success, request.getIndexName());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] ES批量同步异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES批量同步异常，触发本地重试", e);
        }
    }

    /**
     * 监听数据删除请求，从 ES 搜索引擎中删除对应文档
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_DELETE_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_EXCHANGE),
                    key = SearchMqConstants.ES_ROUTING_DELETE)
    })
    public void handleDelete(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] ES删除消息缺失 messageId，拒绝消费. dataId={}", request.getId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "delete:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES delete failed, strategy not found for type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean success = strategy.delete(request.getIndexName(), request.getId().toString());
            log.info("ES delete result: {}, index: {}, id: {}", success, request.getIndexName(), request.getId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] ES删除处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES删除处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听用户资源插入请求（如文章、视频发布），将关键信息推送到 ES
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_USER_RESOURCE_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_EXCHANGE),
                    key = {
                            SearchMqConstants.ES_ROUTING_USER_RESOURCE_INSERT,
                    })
    })
    public void handleUserResourceInsert(UserResourceMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] ES用户资源消息缺失 messageId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "ur:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getSourceType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES user resource insert failed, strategy not found for type={}", request.getSourceType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean success = strategy.insertUserResource(request);
            log.info("ES user resource insert result: {}, sourceId: {}", success, request.getSourceId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] ES用户资源同步异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES用户资源同步异常，触发本地重试", e);
        }
    }

    private boolean isDefaultStrategy(EsSyncStrategy strategy) {
        return strategy == null || Integer.valueOf(-1).equals(strategy.getType());
    }

    /**
     * 监听 ES 同步异常导致的死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = SearchMqConstants.SEARCH_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = SearchMqConstants.SEARCH_DEAD_LETTER_EXCHANGE_NAME),
            key = SearchMqConstants.SEARCH_DEAD_LETTER_ROUTING
    ))
    public void handleSearchDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("search dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
