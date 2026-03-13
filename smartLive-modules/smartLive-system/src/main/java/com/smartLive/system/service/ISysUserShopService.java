package com.smartLive.system.service;

import java.util.List;

/**
 * 用户店铺关系服务
 */
public interface ISysUserShopService
{
    /**
     * 新增用户店铺关系
     */
    boolean addUserShopRelation(Long userId, Long shopId);

    /**
     * 根据店铺ID删除关系
     */
    boolean deleteUserShopRelationByShopId(Long shopId);

    /**
     * 根据店铺ID集合删除关系
     */
    boolean deleteUserShopRelationByShopIds(Long[] shopIds);

    /**
     * 根据用户ID查询店铺ID列表
     */
    List<Long> getShopIdsByUserId(Long userId);
}
