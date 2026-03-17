package com.smartLive.shop.controller;

import java.util.List;

import com.smartLive.common.core.context.SecurityContextHolder;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.shop.domain.VO.ShopVO;
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
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
 * 店铺管理控制器。
 */
@RestController
@RequestMapping("/shop")
public class ShopController extends BaseController {
    @Autowired
    private IShopService shopService;
    /**
     * 分页查询店铺列表。
     */
    @RequiresPermissions("business:shop:list")
    @GetMapping("/list")
    public TableDataInfo list(Shop shop) {
        startPage();
        List<Shop> list = shopService.selectShopList(shop);
        return getDataTable(list);
    }
    /**
     * 查询店铺列表详情。
     */
    @GetMapping("/shopList")
    public AjaxResult shopList(Shop shop) {
        List<Shop> list = shopService.selectShopList(shop);
        return success(list);
    }
    /**
     * 修改店铺信息。
     */
    @RequiresPermissions("business:shop:edit")
    @Log(title = "shop", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult update(@RequestBody Shop shop) {
        return toAjax(shopService.updateShop(shop));
    }
    /**
     * 根据ID获取店铺详情。
     */
    @RequiresPermissions("business:shop:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(shopService.selectShopById(id));
    }
    /**
     * 新增店铺信息。
     */
    @RequiresPermissions("business:shop:add")
    @Log(title = "shop", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Shop shop) {
        return toAjax(shopService.insertShop(shop));
    }
    /**
     * 删除店铺信息。
     */
    @RequiresPermissions("business:shop:remove")
    @Log(title = "shop", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(shopService.deleteShopByIds(ids));
    }
    /**
     * 根据名称和区域分页查询店铺。
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "area",required = false) String area,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(area), "address", area)
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }
    /**
     * 刷新店铺缓存。
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(shopService.flushCache());
    }
    /**
     * 发布店铺数据到搜索服务。
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(shopService.allPublish());
    }
    /**
     * 按ID发布店铺数据到搜索服务。
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable("ids") String[] ids) {
        return success(shopService.publish(ids));
    }
    /**
     * 获取热门店铺排行榜。
     */
    @GetMapping("/hot/rank")
    public Result getHotShopRank(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y) {
        return Result.ok(shopService.getHotShopRank(current, size, x, y));
    }
    /**
     * 根据ID获取店铺详情。
     */
    @GetMapping("/getShopById/{id}")
    public Result getShopById(@PathVariable("id") Long id) {
        ShopVO shop = shopService.queryById(id);
        if (shop == null) {
            return Result.fail("shop not found");
        }
        return Result.ok(shop);
    }
    /**
     * 根据ID集合批量查询店铺。
     */
    @GetMapping("/listByIds")
    public Result queryShopByIds(@RequestParam("ids") String ids) {
        if (StrUtil.isBlank(ids)) {
            return Result.ok(new java.util.ArrayList<>());
        }
        java.util.List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toList());
        List<ShopVO> shops = shopService.getShopList(idList);
        if (shops == null) {
            return Result.ok(new java.util.ArrayList<>());
        }
        shops = shops.stream()
                .filter(shop -> shop != null
                        && java.util.Objects.equals(shop.getStatus(), 1)
                        && java.util.Objects.equals(shop.getAuditStatus(), AuditStatusEnum.PASS.getCode()))
                .collect(java.util.stream.Collectors.toList());
        return Result.ok(shops);
    }
    /**
     * 按关键词搜索店铺。
     */
    @GetMapping("/search")
    public Result searchShops(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.ok(shopService.searchShops(keyword));
    }
}
