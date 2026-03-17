package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.ProductDoc;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.List;

/**
 * Milvus同步策略类。
 */
public interface MilvusSyncStrategy<T> {

    /**
     * 获取策略的类型
     */
    Integer getType();


    /**
     * 单条插入或更新
     * @param id 文档ID
     * @param data 实体数据
     * @return 是否成功
     */
    boolean insertOrUpdate(String id, Object data) throws IOException;

    /**
     * 批量插入
     * @param dataList 实体列表
     *
     * @return 是否成功
     */
    boolean batchInsert(List<Object> dataList) throws IOException;

    /**
     * 按ID删除
     * @param id 文档ID
     * @return 是否成功
     */
    boolean delete(String id) throws IOException;

    /**
     *创建文档
     * @param data
     * @return
     */
    public Document createDocument(T data);
}
