
package com.smartLive.ai.service.business;

import java.io.IOException;
import java.util.List;

/**
 * ES操作基础接口（所有实体类的ES操作都需实现此接口）
 * @param <T> 实体类型
 */
public interface IMilvusBaseService<T> {

    /**
     * 单条插入或更新
     * @param id 文档ID
     * @param data 实体数据
     * @return 是否成功
     */
    boolean insertOrUpdate(String id, T data) throws IOException;

    /**
     * 批量插入
     * @param dataList 实体列表
     *
     * @return 是否成功
     */
    boolean batchInsert(List<T> dataList) throws IOException;

    /**
     * 按ID删除
     * @param id 文档ID
     * @return 是否成功
     */
    boolean delete(String id) throws IOException;
}