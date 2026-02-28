package com.smartLive.ai.listener;
import com.smartLive.common.core.constant.mq.SearchMqConstants;

import com.smartLive.ai.strategy.factory.MilvusSyncFactory;
import com.smartLive.ai.strategy.milvus.MilvusSyncStrategy;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
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
public class MilvusSyncListener {

    @Autowired
    private ExecutorService executorService;
    @Autowired
    private MilvusSyncFactory milvusSyncFactory;

    /**
     * Milvus single insert.
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.MILVUS_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = SearchMqConstants.MILVUS_EXCHANGE),
                    key = SearchMqConstants.MILVUS_ROUTING_INSERT)
    })
    public void handleSingleInsert(ContentSyncMessage request) {
        executorService.submit(() -> {
            log.info("Receive Milvus single insert request: {}", request);
            MilvusSyncStrategy strategy = milvusSyncFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("Milvus single insert failed, strategy not found for type={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.insertOrUpdate(request.getId().toString(), request.getData());
                log.info("Milvus single insert result: {}, type={}", success, request.getType());
            } catch (Exception e) {
                log.error("Milvus single insert exception", e);
            }
        });
    }

    /**
     * Milvus batch insert.
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.MILVUS_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = SearchMqConstants.MILVUS_EXCHANGE),
                    key = SearchMqConstants.MILVUS_ROUTING_BATCH_INSERT)
    })
    public void handleBatchInsert(ContentBatchSyncMessage request) {
        executorService.submit(() -> {
            log.info("Receive Milvus batch insert request: {}", request);
            MilvusSyncStrategy strategy = milvusSyncFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("Milvus batch insert failed, strategy not found for type={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.batchInsert((List<Object>) request.getData());
                log.info("Milvus batch insert result: {}, type={}", success, request.getType());
            } catch (Exception e) {
                log.error("Milvus batch insert exception", e);
            }
        });
    }

    /**
     * Milvus delete.
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = SearchMqConstants.MILVUS_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = SearchMqConstants.MILVUS_EXCHANGE),
                    key = SearchMqConstants.MILVUS_ROUTING_DELETE)
    })
    public void handleDelete(ContentSyncMessage request) {
        executorService.submit(() -> {
            log.info("Receive Milvus delete request, id={}", request.getId());
            MilvusSyncStrategy strategy = milvusSyncFactory.getStrategy(request.getType());
            if (isDefaultStrategy(strategy)) {
                log.error("Milvus delete failed, strategy not found for type={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.delete(request.getId().toString());
                log.info("Milvus delete result: {}", success);
            } catch (Exception e) {
                log.error("Milvus delete exception", e);
            }
        });
    }

    private boolean isDefaultStrategy(MilvusSyncStrategy strategy) {
        return strategy == null || Integer.valueOf(-1).equals(strategy.getType());
    }
}
