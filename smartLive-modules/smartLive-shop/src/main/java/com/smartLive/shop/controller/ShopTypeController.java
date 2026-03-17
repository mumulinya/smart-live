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
 * 店铺类型控制器。
 */
@RestController
@RequestMapping("/shop/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 查询店铺类型列表。
     */
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryList();
    }
}
