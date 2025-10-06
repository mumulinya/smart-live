package com.smartLive.order.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.order.domain.VoucherOrder;
import com.smartLive.order.service.IVoucherOrderService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 优惠券订单表Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController extends BaseController
{
    @Autowired
    private IVoucherOrderService voucherOrderService;

    /**
     * 查询优惠券订单表列表
     */
    @RequiresPermissions("business:order:list")
    @GetMapping("/list")
    public TableDataInfo list(VoucherOrder voucherOrder)
    {
        startPage();
        List<VoucherOrder> list = voucherOrderService.selectVoucherOrderList(voucherOrder);
        return getDataTable(list);
    }

    /**
     * 导出优惠券订单表列表
     */
    @RequiresPermissions("business:order:export")
    @Log(title = "优惠券订单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VoucherOrder voucherOrder)
    {
        List<VoucherOrder> list = voucherOrderService.selectVoucherOrderList(voucherOrder);
        ExcelUtil<VoucherOrder> util = new ExcelUtil<VoucherOrder>(VoucherOrder.class);
        util.exportExcel(response, list, "优惠券订单表数据");
    }

    /**
     * 获取优惠券订单表详细信息
     */
    @RequiresPermissions("business:order:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(voucherOrderService.selectVoucherOrderById(id));
    }

    /**
     * 新增优惠券订单表
     */
    @RequiresPermissions("business:order:add")
    @Log(title = "优惠券订单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VoucherOrder voucherOrder)
    {
        return toAjax(voucherOrderService.insertVoucherOrder(voucherOrder));
    }

    /**
     * 修改优惠券订单表
     */
    @RequiresPermissions("business:order:edit")
    @Log(title = "优惠券订单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VoucherOrder voucherOrder)
    {
        return toAjax(voucherOrderService.updateVoucherOrder(voucherOrder));
    }

    /**
     * 删除优惠券订单表
     */
    @RequiresPermissions("business:order:remove")
    @Log(title = "优惠券订单表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(voucherOrderService.deleteVoucherOrderByIds(ids));
    }
    /**
     * 秒杀优惠券
     */
    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
    /**
     * 购买优惠券
     */
    @PostMapping("/buy/{id}")
    public Result buyVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.buyVoucher(voucherId);
    }
}
