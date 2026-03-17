package com.smartLive.ai.strategy.milvus;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Default MilvusSyncStrategy.
 */
@Component
public class DefaultMilvusSyncStrategy implements MilvusSyncStrategy<Object> {

    /**
     * 获取类型。
     */
    @Override
    public Integer getType() {
        return -1;
    }

    /**
     * 获取结果。
     */
    @Override
    public boolean insertOrUpdate(String id, Object data) throws IOException {
        return false;
    }

    /**
     * 获取结果。
     */
    @Override
    public boolean batchInsert(List<Object> dataList) throws IOException {
        return false;
    }

    /**
     * 删除结果。
     */
    @Override
    public boolean delete(String id) throws IOException {
        return false;
    }

    /**
     * 创建文档。
     */
    @Override
    public Document createDocument(Object data) {
        return null;
    }
}
