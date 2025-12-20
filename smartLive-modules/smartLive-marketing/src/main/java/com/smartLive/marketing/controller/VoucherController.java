package com.smartLive.marketing.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

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
 * @author ruoyi
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController extends BaseController {
    @Autowired
    private IVoucherService voucherService;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;

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
//    @GetMapping(value = "/{id}")
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
     * 删除优惠券
     */
    @RequiresPermissions("marketing:voucher:remove")
    @Log(title = "优惠券", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
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
        return voucherService.queryVoucherOfShop(shopId);
    }

    @GetMapping(value = "/{id}")
    public Result getVoucherById(@PathVariable("id") Long id) {
        return Result.ok(voucherService.selectVoucherById(id));
    }

    /**
     * 秒杀优惠券
     */
    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        return voucherService.seckillVoucher(voucherId, userId);
    }
    /**
     * 购买优惠券
     */
    @PostMapping("/buy/{id}")
    public Result buyVoucher(@PathVariable("id") Long voucherId) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        return voucherService.buyVoucher(voucherId, userId);
    }

    /**
     *   秒杀优惠券(ai代买)
     */
    @PostMapping("/orderSeckillVoucher")
    public Result orderSeckillVoucher(@RequestParam("id") Long voucherId, @RequestParam("userId") Long userId) {
        return voucherService.seckillVoucher(voucherId, userId);
    }
    /**
     * 购买优惠券(ai代买)
     */
    @PostMapping("/orderVoucher")
    public Result orderVoucher(@RequestParam("id") Long voucherId,@RequestParam("userId") Long userId) {
        return voucherService.buyVoucher(voucherId, userId);
    }

    /**
     * 更新优惠券库存
     *
     * @param voucherId 优惠券id
     * @return
     */
    @PostMapping("/{voucherId}")
    public R<Boolean> updateVoucherStatus(@PathVariable("voucherId") Long voucherId) {
        return seckillVoucherService.updateSeckillVoucherByVoucherId(voucherId);
    }
    /**
     * 恢复秒杀券库存
     */
    @PostMapping("/recover/{id}")
    R<Boolean> recoverVoucherStock(@PathVariable("id") Long voucherId){
        return seckillVoucherService.recoverVoucherStock(voucherId);
    }
    @PostMapping("/listSeckillVoucherByVoucher")
    public List<Voucher> listSeckillVoucher(@RequestBody Voucher voucher) {
        return voucherService.listSeckillVoucher(voucher);
    }

    @GetMapping("/listVoucher")
    public List<Voucher> listVoucher() {
        return voucherService.listVoucher();
    }
    /**
     * 获取代金券总数
     */
    @GetMapping("/total")
    R<Integer> getCouponTotal(){
        return R.ok(voucherService.getCouponTotal());
    }
    /**
     * 获取优惠券列表
     */
    @GetMapping("/getVoucherListByIds")
    R<List<Voucher>> getVoucherListByIds(List<Long> sourceIdList){
        return R.ok(voucherService.getVoucherListByIds(sourceIdList));
    }
}
