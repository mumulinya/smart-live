package com.smartLive.interaction.service;

import com.smartLive.common.core.domain.ScrollResult;

/**
 * 动态Feed流Service接口
 *
 * @author mumulin
 * @date 2025-09-21
 */
public interface IFeedService {

    /**
     * 查询用户Feed流列表（滚动分页）
     *
     * @param feedType Feed类型
     * @param max      上一页最大时间戳（用于滚动分页）
     * @param offset   偏移量
     * @return 滚动分页结果
     */
    ScrollResult queryFeedList(Integer feedType, Long max, Integer offset);
}
