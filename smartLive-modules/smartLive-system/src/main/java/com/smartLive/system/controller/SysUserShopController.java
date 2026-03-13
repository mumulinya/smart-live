package com.smartLive.system.controller;

import com.smartLive.system.service.ISysUserShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户店铺关系内部接口
 */
@RestController
@RequestMapping("/user/shop")
public class SysUserShopController
{
    @Autowired
    private ISysUserShopService sysUserShopService;

    /**
     * 新增用户店铺关系
     */
    @PostMapping("/relation")
    public Boolean addUserShopRelation(@RequestParam("userId") Long userId,
                                       @RequestParam("shopId") Long shopId)
    {
        return sysUserShopService.addUserShopRelation(userId, shopId);
    }

    /**
     * 根据用户ID查询店铺ID列表
     */
    @GetMapping("/ids/{userId}")
    public List<Long> getShopIdsByUserId(@PathVariable("userId") Long userId)
    {
        return sysUserShopService.getShopIdsByUserId(userId);
    }

    /**
     * 根据店铺ID删除关系
     */
    @DeleteMapping("/relation/{shopId}")
    public Boolean deleteUserShopRelationByShopId(@PathVariable("shopId") Long shopId)
    {
        return sysUserShopService.deleteUserShopRelationByShopId(shopId);
    }

    /**
     * 根据店铺ID集合批量删除关系
     */
    @PostMapping("/relation/deleteBatch")
    public Boolean deleteUserShopRelationByShopIds(@RequestBody Long[] shopIds)
    {
        return sysUserShopService.deleteUserShopRelationByShopIds(shopIds);
    }
}