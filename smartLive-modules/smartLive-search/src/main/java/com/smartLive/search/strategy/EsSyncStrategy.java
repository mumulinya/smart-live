package com.smartLive.search.strategy;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public interface EsSyncStrategy {

    /**
     * 获取策略支持的数据类型，例如 "voucher", "shop"
     */
    String getDataType();
    /**
     * 单条插入或更新
     * @param indexName
     * @param id
     * @param data
     * @return
     * @throws IOException
     */
    boolean insertOrUpdate(String indexName, String id, Object data) throws IOException;

    /**
     * 批量插入
     * @param indexName ES索引名
     * @param dataList 实体列表
     * @return 是否成功
     */
    boolean batchInsert(String indexName, List<Object> dataList) throws IOException;

    /**
     * 按ID删除
     * @param indexName ES索引名
     * @param id 文档ID
     * @return 是否成功
     */
    boolean delete(String indexName, String id) throws IOException;
}
