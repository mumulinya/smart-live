package com.smartLive.search.listener;

import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.search.strategy.EsSyncStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class EsSyncListener {
    @Autowired
    private Map<Integer, EsSyncStrategy> esStrategyMap;
    @Autowired
    private ExecutorService executorService;
    /**
     * 处理单条插入
     *
     * @param request
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_INSERT)
    })
    public void handleSingleInsert(ContentSyncMessage request) {
       executorService.submit(()->{
           log.info("Es接收单条插入请求: {}", request);
           // 1. 获取策略
           EsSyncStrategy strategy = esStrategyMap.get(request.getType());
           if (strategy == null) {
               log.error("Es单条插入失败：未找到策略 dataType={}", request.getType());
               return;
           }
           try {
               // 2. 直接委托给策略执行
               boolean success = strategy.insertOrUpdate(request.getIndexName(),request.getId().toString(), request.getData());
               log.info("Es单条插入结果: {}, type={}", success, request.getType());
           } catch (Exception e) {
               log.error("Es单条插入异常", e);
           }
       });
    }

    /**
     * 处理批量插入
     *
     * @param request
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_BATCH_INSERT)
    })
    public void handleBatchInsert(ContentBatchSyncMessage request) {
        executorService.submit(()->{
            log.info("Es接收批量插入请求: {}", request);
            log.info("esStrategyMap为：{}", esStrategyMap);
            EsSyncStrategy strategy = esStrategyMap.get(request.getType());
            if (strategy == null) {
                log.error("Es批量插入失败：未找到策略 dataType={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.batchInsert(request.getIndexName(),(List<Object>) request.getData());
                log.info("Es批量插入结果: {}, type={}", success, request.getType());
            } catch (Exception e) {
                log.error("Es批量插入异常", e);
            }
        });
    }

    /**
     * 处理删除
     *
     * @param request
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_DELETE)
    })
    public void handleDelete(ContentSyncMessage request) {
       executorService.submit(()->{
           log.info("Es接收删除请求: id={}", request.getId());
           EsSyncStrategy strategy = esStrategyMap.get(request.getType());
           if (strategy == null) {
               log.error("Es删除失败：未找到策略 dataType={}", request.getType());
               return;
           }
           try {
               boolean success = strategy.delete(request.getIndexName(),request.getId().toString());
               log.info("Es删除结果: {}", success);
           } catch (Exception e) {
               log.error("Es删除异常", e);
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
        executorService.submit(()->{
            log.info("Es接收单条插入请求: {}", request);
            // 1. 获取策略
            EsSyncStrategy strategy = esStrategyMap.get(request.getSourceType());
            if (strategy == null) {
                log.error("Es单条插入失败：未找到策略 dataType={}", request.getSourceType());
                return;
            }
            try {
                // 2. 直接委托给策略执行
                boolean success = strategy.insertUserResource(request);
                log.info("Es单条插入结果: {}, type={}", success, request.getSourceType());
            } catch (Exception e) {
                log.error("Es单条插入异常", e);
            }
        });
    }
}