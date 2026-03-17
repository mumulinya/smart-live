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
     * 获取策略类型。
     */
    Integer getType();


    /**
     * 单条插入或更新。
     *
     * @param id 文档编号
     * @param data 实体数据
     * @return 是否成功
     */
    boolean insertOrUpdate(String id, Object data) throws IOException;

    /**
     * 批量插入数据。
     *
     * @param dataList 实体列表
     * @return 是否成功
     */
    boolean batchInsert(List<Object> dataList) throws IOException;

    /**
     * 按编号删除。
     *
     * @param id 文档编号
     * @return 是否成功
     */
    boolean delete(String id) throws IOException;

    /**
     * 创建文档对象。
     *
     * @param data 实体数据
     * @return 文档对象
     */
    Document createDocument(T data);
}
