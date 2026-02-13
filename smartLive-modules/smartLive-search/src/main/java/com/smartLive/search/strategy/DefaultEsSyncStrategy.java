package com.smartLive.search.strategy;

import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Default EsSyncStrategy.
 */
@Component
public class DefaultEsSyncStrategy implements EsSyncStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        return false;
    }

    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        return false;
    }

    @Override
    public boolean delete(String indexName, String id) throws IOException {
        return false;
    }

    @Override
    public boolean insertUserResource(UserResourceMessage request) throws IOException {
        return false;
    }

    @Override
    public boolean updateUserResource(UserResourceMessage userResourceMessage) throws IOException {
        return false;
    }

    @Override
    public boolean deleteUserResourceBySource(String indexName, Long sourceId, Integer sourceType) throws IOException {
        return false;
    }
}
