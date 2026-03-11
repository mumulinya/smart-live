package com.smartLive.search.strategy.esSync;

import com.smartLive.common.rabbitmq.domain.UserResourceMessage;

import java.io.IOException;
import java.util.List;

/**
 * Elasticsearch 数据同步策略接口
 * 定义了不同业务模型（如博客、商品、店铺等）同步到 ES 索引的标准行为。
 * 支持单条更新、批量同步、物理删除以及特定业务（如社交资源）的宽表注入。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
public interface EsSyncStrategy {

    /**
     * 获取当前策略对应的业务类型标识
     * 用于策略工厂 (EsSyncStrategyFactory) 进行动态分发。
     * 
     * @return 业务类型枚举 Code
     */
    Integer getType();

    /**
     * 单条数据插入或覆盖更新
     * 
     * @param indexName 目标 ES 索引名
     * @param id 文档唯一 ID
     * @param data 业务实体对象（将被转换为 JSON）
     * @return 是否同步成功
     * @throws IOException
     */
    boolean insertOrUpdate(String indexName, String id, Object data) throws IOException;

    /**
     * 批量数据同步
     * 适用于系统初始化或大规模修复数据场景，利用 ES Bulk API 提升吞吐。
     *
     * @param indexName ES 索引名
     * @param dataList 待同步的业务对象列表
     * @return 是否批量执行成功
     */
    boolean batchInsert(String indexName, List<Object> dataList) throws IOException;

    /**
     * 根据文档 ID 执行删除
     *
     * @param indexName ES 索引名
     * @param id 文档 ID
     * @return 是否删除成功
     */
    boolean delete(String indexName, String id) throws IOException;

    /**
     * 插入用户社交资源（宽表聚合同步）
     * 针对如“用户发布的视频/动态”等需要关联用户信息的特殊同步场景。
     */
    default boolean insertUserResource(UserResourceMessage request) throws IOException {
        return true;
    }

    /**
     * 更新用户关联资源
     */
    default boolean updateUserResource(UserResourceMessage userResourceMessage) throws IOException {
        return true;
    }

    /**
     * 根据来源 ID 物理删除关联资源文档
     *
     * @param indexName 索引名
     * @param sourceId 原始业务 ID
     * @param sourceType 业务类型
     */
    default boolean deleteUserResourceBySource(String indexName, Long sourceId, Integer sourceType) throws IOException {
     return true;
    }
}

