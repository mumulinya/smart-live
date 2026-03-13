package com.smartLive.system.service.impl;

import com.smartLive.system.domain.SysUserShop;
import com.smartLive.system.mapper.SysUserShopMapper;
import com.smartLive.system.service.ISysUserShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户店铺关系服务实现
 */
@Service
public class SysUserShopServiceImpl implements ISysUserShopService
{
    @Autowired
    private SysUserShopMapper sysUserShopMapper;

    @Override
    public boolean addUserShopRelation(Long userId, Long shopId)
    {
        if (userId == null || shopId == null)
        {
            return false;
        }
        SysUserShop relation = new SysUserShop();
        relation.setUserId(userId);
        relation.setShopId(shopId);
        // INSERT IGNORE 场景下，新增返回1，重复返回0，均视为成功
        return sysUserShopMapper.insertUserShop(relation) >= 0;
    }

    @Override
    public boolean deleteUserShopRelationByShopId(Long shopId)
    {
        if (shopId == null)
        {
            return false;
        }
        // 删除0条也视为成功，避免幂等删除失败
        return sysUserShopMapper.deleteUserShopByShopId(shopId) >= 0;
    }

    @Override
    public boolean deleteUserShopRelationByShopIds(Long[] shopIds)
    {
        if (shopIds == null || shopIds.length == 0)
        {
            return true;
        }
        // 删除0条也视为成功，避免幂等删除失败
        return sysUserShopMapper.deleteUserShopByShopIds(shopIds) >= 0;
    }

    @Override
    public List<Long> getShopIdsByUserId(Long userId)
    {
        if (userId == null)
        {
            return Collections.emptyList();
        }
        List<Long> shopIds = sysUserShopMapper.selectShopIdsByUserId(userId);
        return shopIds == null ? Collections.emptyList() : shopIds;
    }
}
