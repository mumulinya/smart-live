package com.smartLive.search.listener;

import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.rabbitmq.domain.SearchIndexBatchMessage;
import com.smartLive.common.rabbitmq.domain.SearchIndexMessage;
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

    // ==================== 单条插入 ====================
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = {
                            MqConstants.ES_ROUTING_VOUCHER_INSERT,
                            MqConstants.ES_ROUTING_USER_INSERT,
                            MqConstants.ES_ROUTING_SHOP_INSERT,
                            MqConstants.ES_ROUTING_BLOG_INSERT
                    })
    })
    public void handleSingleInsert(SearchIndexMessage request) {
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

    // ==================== 批量插入 ====================
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = {
                            MqConstants.ES_ROUTING_VOUCHER_BATCH_INSERT,
                            MqConstants.ES_ROUTING_SHOP_BATCH_INSERT,
                            MqConstants.ES_ROUTING_USER_BATCH_INSERT,
                            MqConstants.ES_ROUTING_BLOG_BATCH_INSERT
                    })
    })
    public void handleBatchInsert(SearchIndexBatchMessage request) {
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

    // ==================== 删除 ====================
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = {
                            MqConstants.ES_ROUTING_VOUCHER_DELETE,
                            MqConstants.ES_ROUTING_USER_DELETE,
                            MqConstants.ES_ROUTING_BLOG_DELETE,
                            MqConstants.ES_ROUTING_SHOP_DELETE
                    })
    })
    public void handleDelete(SearchIndexMessage request) {
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
}