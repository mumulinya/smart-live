package com.smartLive.marketing.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.service.ISeckillVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.marketing.domain.Voucher;
import com.smartLive.marketing.service.IVoucherService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 优惠券Controller
 * 
 * @author 木木林
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController extends BaseController {
    @Autowired
    private IVoucherService voucherService;

    /**
     * 查询优惠券列表
     */
    @RequiresPermissions("marketing:voucher:list")
    @GetMapping("/list")
    public TableDataInfo list(Voucher voucher) {
        startPage();
        List<Voucher> list = voucherService.selectVoucherList(voucher);
        return getDataTable(list);
    }


    @GetMapping("/voucherList")
    public AjaxResult voucherList(Voucher voucher) {
        List<Voucher> list = voucherService.selectVoucherList(voucher);
        return success(list);
    }
    /**
     * 导出优惠券列表
     */
    @RequiresPermissions("marketing:voucher:export")
    @Log(title = "优惠券", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Voucher voucher) {
        List<Voucher> list = voucherService.selectVoucherList(voucher);
        ExcelUtil<Voucher> util = new ExcelUtil<Voucher>(Voucher.class);
        util.exportExcel(response, list, "优惠券数据");
    }

    /**
     * 获取优惠券详细信息
     */
    @RequiresPermissions("marketing:voucher:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(voucherService.selectVoucherById(id));
    }

    /**
     * 新增优惠券
     */
    @RequiresPermissions("marketing:voucher:add")
    @Log(title = "优惠券", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Voucher voucher) {
        return toAjax(voucherService.insertVoucher(voucher));
    }

    /**
     * 修改优惠券
     */
    @RequiresPermissions("marketing:voucher:edit")
    @Log(title = "优惠券", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Voucher voucher) {
        return toAjax(voucherService.updateVoucher(voucher));
    }
    /**
     * 代金券新增库存
     */
    @PostMapping("/addStock/{id}")
    public AjaxResult addStock(@PathVariable("id") Long id) {
        return toAjax(voucherService.addStock(id));
    }
    /**
     * 代金券降价
     */
    @PostMapping("/priceReduced/{id}")
    public AjaxResult priceReduced(@PathVariable("id") Long id) {
        return toAjax(voucherService.priceReduced(id));
    }
    /**
     * 修改代金券状态
     */
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody Voucher voucher) {
        return toAjax(voucherService.changeStatus(voucher));
    }
    /**
     * 删除优惠券
     */
    @RequiresPermissions("marketing:voucher:remove")
    @Log(title = "优惠券", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(voucherService.deleteVoucherByIds(ids));
    }


    /**
     * 全量发布代金券信息
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(voucherService.allPublish());
    }

    /**
     * 发布代金券信息
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(voucherService.publish(ids));
    }

    /**
     * 新增秒杀券
     *
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     *
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        List<Voucher> voucherList = voucherService.queryVoucherOfShop(shopId);
        return Result.ok(voucherList);
    }

    /**
     * 根据代金券id获取代金券
     * @param id
     * @return
     */
    @GetMapping(value = "/getVoucherById/{id}")
    public Result getVoucherById(@PathVariable("id") Long id) {
        return Result.ok(voucherService.getVoucherById(id));
    }

    /**
     * 秒杀优惠券
     */
    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        Long orderId = voucherService.seckillVoucher(voucherId, userId);
        return Result.ok(orderId);
    }
    /**
     * 购买优惠券
     */
    @PostMapping("/buy/{id}")
    public Result buyVoucher(@PathVariable("id") Long voucherId) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(voucherService.buyVoucher(voucherId, userId));
    }
}
