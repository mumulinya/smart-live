package com.smartLive.search.service;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * ES操作基础接口（所有实体类的ES操作都需实现此接口）
 * @param <T> 实体类型
 */
public interface IEsBaseService<T> {

    /**
     * 单条插入或更新
     * @param indexName ES索引名
     * @param id 文档ID
     * @param data 实体数据
     * @return 是否成功
     */
    boolean insertOrUpdate(String indexName, String id, T data) throws IOException;

    /**
     * 批量插入
     * @param indexName ES索引名
     * @param dataList 实体列表
     * @param idGenerator 文档ID生成器（从实体中提取ID）
     * @return 是否成功
     */
    boolean batchInsert(String indexName, List<T> dataList, Function<T, String> idGenerator) throws IOException;

    /**
     * 按ID删除
     * @param indexName ES索引名
     * @param id 文档ID
     * @return 是否成功
     */
    boolean delete(String indexName, String id) throws IOException;
}