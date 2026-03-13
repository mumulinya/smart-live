package com.smartLive.system.mapper;

import com.smartLive.system.domain.SysUserShop;

import java.util.List;

/**
 * 用户店铺关系数据层
 */
public interface SysUserShopMapper
{
    /**
     * 新增用户店铺关系
     */
    int insertUserShop(SysUserShop userShop);

    /**
     * 根据店铺ID删除关系
     */
    int deleteUserShopByShopId(Long shopId);

    /**
     * 根据店铺ID集合批量删除关系
     */
    int deleteUserShopByShopIds(Long[] shopIds);

    /**
     * 根据用户ID查询店铺ID列表
     */
    List<Long> selectShopIdsByUserId(Long userId);
}
