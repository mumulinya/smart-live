package com.smartLive.ai.listener;
import com.smartLive.ai.strategy.milvus.MilvusSyncStrategy;
import com.smartLive.common.core.constant.MqConstants;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class MilvusSyncListener {

    @Autowired
    private ExecutorService executorService;
    @Autowired
    private Map<Integer, MilvusSyncStrategy> milvusStrategyMap;
    /**
     * Milvus单条插入
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.MILVUS_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.MILVUS_EXCHANGE),
                    key = MqConstants.MILVUS_ROUTING_INSERT)
    })
    public void handleSingleInsert(ContentSyncMessage request) {
      executorService.submit(()->{
          log.info("线程：{}接收Milvus单条插入请求: {}",Thread.currentThread().getName(), request);
          // 1. 获取策略
          MilvusSyncStrategy strategy = milvusStrategyMap.get(request.getType());
          if (strategy == null) {
              log.error("Milvus单条插入失败：未找到策略 dataType={}", request.getType());
              return;
          }
          try {
              // 2. 直接委托给策略执行
              boolean success = strategy.insertOrUpdate(request.getId().toString(), request.getData());
              log.info("Milvus单条插入结果: {}, type={}", success, request.getType());
          } catch (Exception e) {
              log.error("Milvus单条插入异常", e);
          }
      });
    }
    /**
     * Milvus批量插入
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.MILVUS_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.MILVUS_EXCHANGE),
                    key = MqConstants.MILVUS_ROUTING_BATCH_INSERT)
    })
    public void handleBatchInsert(ContentBatchSyncMessage request) {
       executorService.submit(()->{
           log.info("线程：{}接收Milvus批量插入请求: {}",Thread.currentThread().getName(), request);
           MilvusSyncStrategy strategy = milvusStrategyMap.get(request.getType());
           if (strategy == null) {
               log.error("Milvus批量插入失败：未找到策略 dataType={}", request.getType());
               return;
           }
           try {
               boolean success = strategy.batchInsert((List<Object>) request.getData());
               log.info("Milvus批量插入结果: {}, type={}", success, request.getType());
           } catch (Exception e) {
               log.error("Milvus批量插入异常", e);
           }
          });
    }
    /**
     * Milvus删除
     */
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.MILVUS_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.MILVUS_EXCHANGE),
                    key = MqConstants.MILVUS_ROUTING_DELETE)
    })
    public void handleDelete(ContentSyncMessage request) {
        executorService.submit(()->{
            log.info("线程：{}接收Milvus删除请求: id={}",Thread.currentThread().getName(), request.getId());
            MilvusSyncStrategy strategy = milvusStrategyMap.get(request.getType());
            if (strategy == null) {
                log.error("Milvus删除失败：未找到策略 dataType={}", request.getType());
                return;
            }
            try {
                boolean success = strategy.delete(request.getId().toString());
                log.info("Milvus删除结果: {}", success);
            } catch (Exception e) {
                log.error("Milvus删除异常", e);
            }
        });
    }
}