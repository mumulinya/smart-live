package com.smartLive.shop.controller;

import java.util.List;

import com.smartLive.shop.domain.VO.ShopVO;
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 店铺Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController

@RequestMapping("/shop")
public class ShopController extends BaseController {
    @Autowired
    private IShopService shopService;

    /**
     * 分页查询店铺列表
     */
    @RequiresPermissions("business:shop:list")
    @GetMapping("/list")
    public TableDataInfo list(Shop shop) {
        startPage();
        List<Shop> list = shopService.selectShopList(shop);
        return getDataTable(list);
    }

    /**
     * 查询店铺列表
     */
    @GetMapping("/shopList")
    public AjaxResult shopList(Shop shop) {
        List<Shop> list = shopService.selectShopList(shop);
        return success(list);
    }
    /**
     * 导出店铺列表
     */
    @RequiresPermissions("business:shop:export")
    @Log(title = "店铺", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Shop shop) {
        List<Shop> list = shopService.selectShopList(shop);
        ExcelUtil<Shop> util = new ExcelUtil<Shop>(Shop.class);
        util.exportExcel(response, list, "店铺数据");
    }

    /**
     * 获取店铺详细信息
     */
    @RequiresPermissions("business:shop:query")
//    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(shopService.selectShopById(id));
    }

    /**
     * 新增店铺
     */
    @RequiresPermissions("business:shop:add")
    @Log(title = "店铺", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Shop shop) {
        return toAjax(shopService.insertShop(shop));
    }

    /**
     * 修改店铺
     */
    @RequiresPermissions("business:shop:edit")
    @Log(title = "店铺", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Shop shop) {
        return toAjax(shopService.updateShop(shop));
    }

    /**
     * 删除店铺
     */
    @RequiresPermissions("business:shop:remove")
    @Log(title = "店铺", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(shopService.deleteShopByIds(ids));
    }
    /**
     * 刷新缓存
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(shopService.flushCache());
    }

    /**
     * 全量发布店铺
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(shopService.allPublish());
    }

    /**
     * 发布店铺
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(shopService.publish(ids));
    }
    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        ShopVO shop = shopService.queryById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }
}