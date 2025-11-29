package com.smartLive.shop.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.domain.R;
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

//@RequestMapping("/shop")
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
     * 根据商铺类型分页查询商铺信息
     *
     * @param typeId  商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "sortBy",defaultValue = "distance") String sortBy,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {

        return shopService.queryShopByType(typeId, current,sortBy, x, y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     *
     * @param name    商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {

        return shopService.queryById(id);
    }

    @PostMapping("/shop/list")
    List<Shop> searchShopsByCategory( @RequestBody Shop shopQuery) {
        return shopService.searchShopsByShopQuery(shopQuery);
    }

    @PostMapping("/shop/detail")
    Shop getShopDetails( @RequestBody Shop shopVO) {
        return shopService.selectShopByShop(shopVO);
    }
    @GetMapping("/shop/{shopName}")
    public R<Shop> getShopByShopName(@PathVariable("shopName") String shopName){
        return shopService.getShopByShopName(shopName);
    }
    /**
     * 更新商家评论数
     */
    @PostMapping("/shop/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long shopId){
        return shopService.updateCommentById(shopId);
    }

    /**
     * 根据条件查询商家信息
     */
    @PostMapping("/shop/getShopList")
    public R<List<Shop>> getShopByCondition(@RequestBody Shop shop){
        return R.ok(shopService.getShopByCondition(shop));
    }

    @GetMapping("/shop/getShopById/{shopId}")
    public R<Shop> getShopById(@PathVariable("shopId") Long shopId ){
        return shopService.getShopById(shopId);
    }

    @GetMapping("/shop/shopListByIds")
     public R<List<Shop>> listShopByIds(@RequestParam("shopIdList") List<Long> shopIdList){
        return R.ok(shopService.getShopList(shopIdList));
    }
    /**
     * 获取商家总数
     */
    @GetMapping("/shop/getShopTotal")
    public R<Integer> getShopTotal() {
        return R.ok(shopService.getShopTotal());
    }
    /**
     * 获取最近创建商家
     */
    @GetMapping("/shop/getRecentShops")
    public R<List<Shop>> getRecentShops(@RequestParam("limit") Integer limit){
        return R.ok(shopService.getRecentShops(limit));
    }
}