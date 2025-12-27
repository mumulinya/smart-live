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
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-2
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryList();
    }
    /**
     * 查询商铺类型列表
     */
    @GetMapping("/getShopListByType")
    public R<List<ShopType>> getShopTypeList(){
        Result result = typeService.queryList();
        List<ShopType> shopTypeList = (List<ShopType>) result.getData();
        return R.ok(shopTypeList);
    }
}
