package com.smartLive.common.core.constant.mq;

/**
 * 搜索与同步模�?MQ 常量（ES与Milvus�?
 */
public interface SearchMqConstants {
    /**
     * ES 交换�?
     */
    String ES_SYNC_EXCHANGE = "es.sync.direct.exchange";
    // 插入队列
    String ES_SYNC_INSERT_QUEUE = "es.sync.insert.queue";
    // 批量插入队列
    String ES_SYNC_BATCH_INSERT_QUEUE = "es.sync.batch.insert.queue";
    // 删除队列
    String ES_SYNC_DELETE_QUEUE = "es.sync.delete.queue";
    // 用户资源队列
    String ES_SYNC_USER_RESOURCE_QUEUE = "es.sync.user.resource.queue";
    // 插入路由�?
    String ES_SYNC_INSERT_ROUTING_KEY = "es.sync.insert";
    String ES_SYNC_USER_RESOURCE_INSERT_ROUTING_KEY = "es.sync.user.resource.insert";
    // 批量插入路由�?
    String ES_SYNC_BATCH_INSERT_ROUTING_KEY = "es.sync.batch.insert";
    // 单个删除路由�?
    String ES_SYNC_DELETE_ROUTING_KEY = "es.sync.delete";

    /**
     * Milvus 交换�?
     */
    String MILVUS_SYNC_EXCHANGE = "milvus.sync.direct.exchange";
    // Milvus 队列名称
    String MILVUS_SYNC_INSERT_QUEUE = "milvus.sync.insert.queue";
    String MILVUS_SYNC_BATCH_INSERT_QUEUE = "milvus.sync.batch.insert.queue";
    String MILVUS_SYNC_DELETE_QUEUE = "milvus.sync.delete.queue";
    // Milvus 路由�?
    String MILVUS_SYNC_INSERT_ROUTING_KEY = "milvus.sync.insert";
    String MILVUS_SYNC_DELETE_ROUTING_KEY = "milvus.sync.delete";
    String MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY = "milvus.sync.batch.insert";

    String SEARCH_DLX_EXCHANGE = "search.dlx.direct.exchange";
    String SEARCH_DLQ_QUEUE = "search.dlq.queue";
    String SEARCH_DLQ_ROUTING_KEY = "search.dlq";
}
