package com.smartLive.ai.listener;

import com.smartLive.ai.entity.request.MilvusBatchInsertRequest;
import com.smartLive.ai.entity.request.MilvusInsertRequest;
import com.smartLive.ai.service.strategy.milvus.MilvusSyncStrategy;
import com.smartLive.common.core.constant.MqConstants;
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
    private Map<String, MilvusSyncStrategy> milvusStrategyMap;

    //基于 Spring 容器管理的策略分发模式
//    @Autowired
//    public MilvusSyncListener(List<MilvusSyncStrategy> strategies) {
//        this.milvusStrategyMap = strategies.stream()
//                .collect(Collectors.toMap(
//                        MilvusSyncStrategy::getDataType,  // 使用 dataType 作为键
//                        Function.identity()               // 策略对象作为值
//                ));
//    }

    // ==================== 单条插入 ====================
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.MILVUS_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.MILVUS_EXCHANGE),
                    key = {MqConstants.MILVUS_ROUTING_VOUCHER_INSERT,
                            MqConstants.MILVUS_ROUTING_USER_INSERT,
                            MqConstants.MILVUS_ROUTING_SHOP_INSERT,
                            MqConstants.MILVUS_ROUTING_BLOG_INSERT})
    })
    public void handleSingleInsert(MilvusInsertRequest request) {
      executorService.submit(()->{
          log.info("线程：{}接收Milvus单条插入请求: {}",Thread.currentThread().getName(), request);
          // 1. 获取策略
          MilvusSyncStrategy strategy = milvusStrategyMap.get(request.getDataType());
          if (strategy == null) {
              log.error("Milvus单条插入失败：未找到策略 dataType={}", request.getDataType());
              return;
          }
          try {
              // 2. 直接委托给策略执行
              boolean success = strategy.insertOrUpdate(request.getId().toString(), request.getData());
              log.info("Milvus单条插入结果: {}, type={}", success, request.getDataType());
          } catch (Exception e) {
              log.error("Milvus单条插入异常", e);
          }
      });
    }
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.MILVUS_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.MILVUS_EXCHANGE),
                    key = {MqConstants.MILVUS_ROUTING_VOUCHER_BATCH_INSERT,
                            MqConstants.MILVUS_ROUTING_USER_BATCH_INSERT,
                            MqConstants.MILVUS_ROUTING_SHOP_BATCH_INSERT,
                            MqConstants.MILVUS_ROUTING_BLOG_BATCH_INSERT})
    })
    public void handleBatchInsert(MilvusBatchInsertRequest request) {
       executorService.submit(()->{
           log.info("线程：{}接收Milvus批量插入请求: {}",Thread.currentThread().getName(), request);
           MilvusSyncStrategy strategy = milvusStrategyMap.get(request.getDataType());
           if (strategy == null) {
               log.error("Milvus批量插入失败：未找到策略 dataType={}", request.getDataType());
               return;
           }
           try {
               boolean success = strategy.batchInsert((List<Object>) request.getData());
               log.info("Milvus批量插入结果: {}, type={}", success, request.getDataType());
           } catch (Exception e) {
               log.error("Milvus批量插入异常", e);
           }
          });
    }

    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.MILVUS_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.MILVUS_EXCHANGE),
                    key = {MqConstants.MILVUS_ROUTING_VOUCHER_DELETE,
                            MqConstants.MILVUS_ROUTING_USER_DELETE,
                            MqConstants.MILVUS_ROUTING_SHOP_DELETE,
                            MqConstants.MILVUS_ROUTING_BLOG_DELETE})
    })
    public void handleDelete(MilvusInsertRequest request) {
        executorService.submit(()->{
            log.info("线程：{}接收Milvus删除请求: id={}",Thread.currentThread().getName(), request.getId());
            MilvusSyncStrategy strategy = milvusStrategyMap.get(request.getDataType());
            if (strategy == null) {
                log.error("Milvus删除失败：未找到策略 dataType={}", request.getDataType());
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