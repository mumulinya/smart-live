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

/**
 * Elasticsearch 数据同步监听器
 * 负责消费来自各个业务模块（博客、商城、互动等）发出的同步指令，实现业务库与索引库的准实时一致性。
 * 集成了分布式幂等控制、多维度同步策略以及死信重试机制。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component
@Slf4j
public class EsSyncListener {

    @Autowired
    private EsSyncStrategyFactory esSyncStrategyFactory;

    @Autowired
    private RedisService redisService;

    /**
     * 监听单条数据同步请求
     * 应用场景：商品新增、博客发布等。
     * 
     * 流程：
     * 1. 消息幂等校验 (基于 messageId)
     * 2. 根据业务类型路由到对应的 EsSyncStrategy 策略实现类
     * 3. 执行 ES 操作并手动 ACK
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_SYNC_INSERT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DLQ_ROUTING_KEY)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_SYNC_EXCHANGE),
                    key = SearchMqConstants.ES_SYNC_INSERT_ROUTING_KEY)
    })
    public void handleSingleInsert(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[ES 同步异常] 缺失全局唯一 messageId，无法保障幂等，拒绝处理. dataId={}", request.getId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "insert:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;

        try {
            // Redis 原子占坑实现防重消费
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[ES 同步重叠] 检测到重复的同步指令，已自动忽略, key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("[ES 同步失败] 未匹配到对应的同步策略，type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 执行具体的索引写入逻辑
            boolean success = strategy.insertOrUpdate(request.getIndexName(), request.getId().toString(), request.getData());
            log.info("ES 索引更新完成: {}, index: {}, id: {}", success, request.getIndexName(), request.getId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[ES 同步崩溃] 处理异常，清除幂等位并触发 MQ 本地重试, key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES 实时同步链路异常", e);
        }
    }

    /**
     * 监听批量数据同步请求
     * 应用场景：存量数据初始化或大批量导入。
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_SYNC_BATCH_INSERT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DLQ_ROUTING_KEY)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_SYNC_EXCHANGE),
                    key = SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY)
    })
    public void handleBatchInsert(ContentBatchSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getData() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[ES 批量同步异常] 消息标识缺失");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "batch:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[ES 批量同步重叠] 已跳过重复指令: {}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("[ES 批量同步失败] 策略未找到 for type={}", request.getType());
                channel.basicAck(deliveryTag, false);
                return;
            }
            @SuppressWarnings("unchecked")
            boolean success = strategy.batchInsert(request.getIndexName(), (List<Object>) request.getData());
            log.info("ES 批量索引同步结果: {}, 索引名: {}", success, request.getIndexName());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[ES 批量同步失败] 触发重试", e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES 批量逻辑异常", e);
        }
    }

    /**
     * 监听数据删除请求
     * 应用场景：内容违规下架、用户销注等。
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_SYNC_DELETE_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DLQ_ROUTING_KEY)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_SYNC_EXCHANGE),
                    key = SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY)
    })
    public void handleDelete(ContentSyncMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null || request.getId() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[ES 删除同步异常] messageId 缺失");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "delete:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean success = strategy.delete(request.getIndexName(), request.getId().toString());
            log.info("ES 文档物理删除结果: {}, id: {}", success, request.getId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[ES 删除同步失败]", e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES 删除逻辑异常", e);
        }
    }

    /**
     * 监听复杂用户资源同步（多表关联后的宽表更新）
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.ES_SYNC_USER_RESOURCE_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = SearchMqConstants.SEARCH_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = SearchMqConstants.SEARCH_DLQ_ROUTING_KEY)
                    }
            ),
                    exchange = @Exchange(name = SearchMqConstants.ES_SYNC_EXCHANGE),
                    key = {
                            SearchMqConstants.ES_SYNC_USER_RESOURCE_INSERT_ROUTING_KEY,
                    })
    })
    public void handleUserResourceInsert(UserResourceMessage request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (request == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[ES 资源同步异常] messageId 缺失");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "ur:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.SEARCH_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getSourceType());
            if (isDefaultStrategy(strategy)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 执行资源注入逻辑
            boolean success = strategy.insertUserResource(request);
            log.info("ES 社交资源同步结果: {}, sourceId: {}", success, request.getSourceId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[ES 资源同步失败]", e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("ES 社交资源同步异常", e);
        }
    }

    private boolean isDefaultStrategy(EsSyncStrategy strategy) {
        return strategy == null || Integer.valueOf(-1).equals(strategy.getType());
    }

    /**
     * 核心死信队列处理器
     * 捕获并记录所有重试耗尽后的同步失败消息，作为人工介入或定时补偿的原始存证。
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = SearchMqConstants.SEARCH_DLQ_QUEUE, durable = "true"),
            exchange = @Exchange(value = SearchMqConstants.SEARCH_DLX_EXCHANGE),
            key = SearchMqConstants.SEARCH_DLQ_ROUTING_KEY
    ))
    public void handleSearchDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("🛑 [ES 同步绝望失败] 已落入死信队列，请关注 body 负载: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
