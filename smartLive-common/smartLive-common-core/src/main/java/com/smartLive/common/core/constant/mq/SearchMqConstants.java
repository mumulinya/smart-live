package com.smartLive.common.core.constant.mq;

/**
 * 搜索与同步模�?MQ 常量（ES与Milvus�?
 */
public interface SearchMqConstants {
    /**
     * ES 交换�?
     */
    String ES_EXCHANGE = "es.sync.exchange";
    // 插入队列
    String ES_INSERT_QUEUE = "es.sync.insert.queue";
    // 批量插入队列
    String ES_BATCH_INSERT_QUEUE = "es.sync.batch.insert.queue";
    // 删除队列
    String ES_DELETE_QUEUE = "es.sync.delete.queue";
    // 用户资源队列
    String ES_USER_RESOURCE_QUEUE = "es.sync.userResource.queue";
    // 插入路由�?
    String ES_ROUTING_INSERT = "es.insert";
    String ES_ROUTING_USER_RESOURCE_INSERT= "es.userResource.insert";
    // 批量插入路由�?
    String ES_ROUTING_BATCH_INSERT = "es.voucher.batch.insert";
    // 单个删除路由�?
    String ES_ROUTING_DELETE = "es.voucher.delete";

    /**
     * Milvus 交换�?
     */
    String MILVUS_EXCHANGE = "milvus.sync.exchange";
    // Milvus 队列名称
    String MILVUS_INSERT_QUEUE = "milvus.sync.insert.queue";
    String MILVUS_BATCH_INSERT_QUEUE = "milvus.sync.batch.insert.queue";
    String MILVUS_DELETE_QUEUE = "milvus.sync.delete.queue";
    // Milvus 路由�?
    String MILVUS_ROUTING_INSERT = "milvus.insert";
    String MILVUS_ROUTING_DELETE = "milvus.delete";
    String MILVUS_ROUTING_BATCH_INSERT = "milvus.batch.insert";

    String SEARCH_DEAD_LETTER_EXCHANGE_NAME = "search.dead.letter.direct";
    String SEARCH_DEAD_LETTER_QUEUE = "search.dead.letter.queue";
    String SEARCH_DEAD_LETTER_ROUTING = "search.dead.letter.routing";
}
