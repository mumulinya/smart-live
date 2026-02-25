package com.smartLive.search.strategy.esSync;

import com.smartLive.common.rabbitmq.domain.UserResourceMessage;

import java.io.IOException;
import java.util.List;

public interface EsSyncStrategy {

    /**
     * 获取策略的类型
     */
    Integer getType();

    /**
     * 单条插入或更新
     *
     * @param indexName
     * @param id
     * @param data
     * @return
     * @throws IOException
     */
    boolean insertOrUpdate(String indexName, String id, Object data) throws IOException;

    /**
     * 批量插入
     *
     * @param indexName ES索引名
     * @param dataList  实体列表
     * @return 是否成功
     */
    boolean batchInsert(String indexName, List<Object> dataList) throws IOException;

    /**
     * 按ID删除
     *
     * @param indexName ES索引名
     * @param id        文档ID
     * @return 是否成功
     */
    boolean delete(String indexName, String id) throws IOException;

    /**
     * @param request
     * @return
     */
    default boolean insertUserResource(UserResourceMessage request) throws IOException {
        return true;
    }

    /**
     * 更新用户资源
     *
     * @param userResourceMessage
     * @return
     */
    default boolean updateUserResource(UserResourceMessage userResourceMessage) throws IOException {
        return true;
    }
    /**
     * 删除用户资源
     *
     * @param indexName
     * @param sourceId
     * @param sourceType
     * @return
     */
    default boolean deleteUserResourceBySource(String indexName, Long sourceId, Integer sourceType) throws IOException {
     return true;
    }
}
