package com.smartLive.shop.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店铺服务内部Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/shop")
public class ShopInnerController extends BaseController {
    @Autowired
    private IShopService shopService;

    @GetMapping("/{shopName}")
    public Shop getShopByShopName(@PathVariable("shopName") String shopName){
        return shopService.getShopByShopName(shopName);
    }
    /**
     * 更新商家评论数
     */
    @PostMapping("/updateCommentById/{id}")
    public Boolean updateCommentById(@PathVariable("id") Long shopId){
        return shopService.updateCommentById(shopId);
    }

    /**
     * 根据条件查询商家信息
     */
    @PostMapping("/getShopList")
    public List<Shop> getShopByCondition(@RequestBody Shop shop){
        return shopService.getShopByCondition(shop);
    }
    /**
     * 根据id查询商家信息
     */
    @GetMapping("/getShopById/{shopId}")
    public Shop getShopById(@PathVariable("shopId") Long shopId ){
        return shopService.queryById(shopId);
    }
    /**
     * 根据id列表查询商家信息
     */
    @GetMapping("/shopListByIds")
     public List<Shop> listShopByIds(@RequestParam("shopIdList") List<Long> shopIdList){
        return shopService.getShopList(shopIdList);
    }
    /**
     * 获取商家总数
     */
    @GetMapping("/getShopTotal")
    public Integer getShopTotal() {
        return shopService.getShopTotal();
    }
    /**
     * 获取最近创建商家
     */
    @GetMapping("/getRecentShops")
    public List<Shop> getRecentShops(@RequestParam("limit") Integer limit){
        return shopService.getRecentShops(limit);
    }
}