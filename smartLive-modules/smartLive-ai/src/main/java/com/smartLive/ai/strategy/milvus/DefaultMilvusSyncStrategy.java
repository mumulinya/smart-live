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

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public boolean insertOrUpdate(String id, Object data) throws IOException {
        return false;
    }

    @Override
    public boolean batchInsert(List<Object> dataList) throws IOException {
        return false;
    }

    @Override
    public boolean delete(String id) throws IOException {
        return false;
    }

    @Override
    public Document createDocument(Object data) {
        return null;
    }
}
