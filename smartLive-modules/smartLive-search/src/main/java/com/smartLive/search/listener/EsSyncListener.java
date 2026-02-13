package com.smartLive.search.listener;

import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.search.strategy.EsSyncStrategy;
import com.smartLive.search.strategy.factory.EsSyncStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class EsSyncListener {

    @Autowired
    private EsSyncStrategyFactory esSyncStrategyFactory;
    @Autowired
    private ExecutorService executorService;

    /**
     * Handle single insert.
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_INSERT)
    })
    public void handleSingleInsert(ContentSyncMessage request) {
        executorService.submit(() -> {
            log.info("ES receive single insert request: {}", request);
            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES single insert failed, strategy not found for type={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.insertOrUpdate(request.getIndexName(), request.getId().toString(), request.getData());
                log.info("ES single insert result: {}, type={}", success, request.getType());
            } catch (Exception e) {
                log.error("ES single insert exception", e);
            }
        });
    }

    /**
     * Handle batch insert.
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_BATCH_INSERT)
    })
    public void handleBatchInsert(ContentBatchSyncMessage request) {
        executorService.submit(() -> {
            log.info("ES receive batch insert request: {}", request);
            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES batch insert failed, strategy not found for type={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.batchInsert(request.getIndexName(), (List<Object>) request.getData());
                log.info("ES batch insert result: {}, type={}", success, request.getType());
            } catch (Exception e) {
                log.error("ES batch insert exception", e);
            }
        });
    }

    /**
     * Handle delete.
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_DELETE)
    })
    public void handleDelete(ContentSyncMessage request) {
        executorService.submit(() -> {
            log.info("ES receive delete request, id={}", request.getId());
            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES delete failed, strategy not found for type={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.delete(request.getIndexName(), request.getId().toString());
                log.info("ES delete result: {}", success);
            } catch (Exception e) {
                log.error("ES delete exception", e);
            }
        });
    }

    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_USER_RESOURCE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = {
                            MqConstants.ES_ROUTING_USER_RESOURCE_INSERT,
                    })
    })
    public void handleUserResourceInsert(UserResourceMessage request) {
        executorService.submit(() -> {
            log.info("ES receive user resource insert request: {}", request);
            EsSyncStrategy strategy = esSyncStrategyFactory.getStrategy(request.getSourceType());
            if (isDefaultStrategy(strategy)) {
                log.error("ES user resource insert failed, strategy not found for type={}", request.getSourceType());
                return;
            }
            try {
                boolean success = strategy.insertUserResource(request);
                log.info("ES user resource insert result: {}, type={}", success, request.getSourceType());
            } catch (Exception e) {
                log.error("ES user resource insert exception", e);
            }
        });
    }

    private boolean isDefaultStrategy(EsSyncStrategy strategy) {
        return strategy == null || Integer.valueOf(-1).equals(strategy.getType());
    }
}
