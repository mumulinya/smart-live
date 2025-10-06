package com.smartLive.map.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.map.service.IMapService;
import com.smartLive.shop.api.domain.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MapController {
    @Autowired
    private IMapService mapService;
    /**
     * 查询商铺类型列表
     * @return
     */
    @GetMapping("/getShopTypeList")
    public Result getShopTypeList(){
        return Result.ok(mapService.queryShopTypeList());
    }
    /**
     * 获取商铺列表
     * @param shopDTO
     * @return
     */
    @GetMapping("/getShopList")
    public Result getShopList(ShopDTO shopDTO){
        System.out.println("查询数据为"+shopDTO);
        if(shopDTO.getKeyword()!=null&&shopDTO.getKeyword().trim().length()>0){
            shopDTO.setName(shopDTO.getKeyword());
            shopDTO.setArea(shopDTO.getKeyword());
            shopDTO.setAddress(shopDTO.getKeyword());
        }
        return Result.ok(mapService.queryShopList(shopDTO));
    }
}
