package com.smartLive.shop.controller;


import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.shop.domain.ShopType;
import com.smartLive.shop.service.IShopTypeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店铺类型内部接口控制器。
 */
@RestController
@RequestMapping("/inner/shop/shop-type")
public class ShopTypeInnerController {
    @Resource
    private IShopTypeService typeService;
    /**
     * 获取店铺类型列表。
     */
    @GetMapping("/getShopListByType")
    public List<ShopType> getShopTypeList(){
        Result result = typeService.queryList();
        List<ShopType> shopTypeList = (List<ShopType>) result.getData();
        return shopTypeList;
    }
}
