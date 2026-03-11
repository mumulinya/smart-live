package com.smartLive.shop.controller;


import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.shop.domain.ShopType;
import com.smartLive.shop.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 店铺类型控制层
 * 用于获取店铺的所有分类列表（如：美食、KTV、电影等）。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/shop/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 查询所有店铺类型列表
     * 通常用于首页金刚区或分类筛选器的初始化。
     *
     * @return 包含店铺类型实体的结果集
     */
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryList();
    }
}
