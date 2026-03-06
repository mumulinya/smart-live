package com.smartLive.order.controller;

import java.util.List;

import com.smartLive.order.domain.VO.OrderVO;
import jakarta.servlet.http.HttpServletResponse;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.order.domain.Order;
import com.smartLive.order.service.IOrderService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 订单表Controller
 *
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/order")
public class OrderController extends BaseController
{
    @Autowired
    private IOrderService orderService;

    /**
     * 查询订单表列表
     */
    @RequiresPermissions("business:order:list")
    @GetMapping("/list")
    public TableDataInfo list(Order order)
    {
        startPage();
        List<Order> list = orderService.selectOrderList(order);
        return getDataTable(list);
    }

    /**
     * 导出订单表列表
     */
    @RequiresPermissions("business:order:export")
    @Log(title = "订单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Order order)
    {
        List<Order> list = orderService.selectOrderList(order);
        ExcelUtil<Order> util = new ExcelUtil<Order>(Order.class);
        util.exportExcel(response, list, "订单表数据");
    }

    /**
     * 获取订单表详细信息
     */
    @RequiresPermissions("business:order:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderService.selectOrderById(id));
    }

    /**
     * 新增订单表
     */
    @RequiresPermissions("business:order:add")
    @Log(title = "订单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Order order)
    {
        return toAjax(orderService.insertOrder(order));
    }

    /**
     * 修改订单表
     */
    @RequiresPermissions("business:order:edit")
    @Log(title = "订单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Order order)
    {
        return toAjax(orderService.updateOrder(order));
    }

    /**
     * 删除订单表
     */
    @RequiresPermissions("business:order:remove")
    @Log(title = "订单表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderService.deleteOrderByIds(ids));
    }


    /**
     * 获取当前用户订单列表
     */
    @GetMapping("/of/me")
    public Result queryMyOrderList(Order order,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        order.setUserId(userId);
        List<OrderVO> orderList = orderService.queryMyOrderList(order, current);
        return Result.ok(orderList);
    }

    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    @GetMapping("/getOrderById/{id}")
    public Result getOrderById(@PathVariable("id") Long id){
        return Result.ok(orderService.getOrderById(id));
    }

    /**
     * 支付订单
     */
     @PostMapping("/pay/{id}")
    public Result pay(@PathVariable("id") Long id) {
         Integer pay = orderService.pay(id);
         if(pay>0){
             return Result.ok("支付成功");
         }
         return Result.fail("支付失败");
    }

    /**
     * 使用订单 (核销)
     */
    @PostMapping("/use")
    public Result use(@RequestBody Order order) {
        Long id = order.getId();
        Long shopId = order.getShopId();
        Integer use = orderService.use(id, shopId);
        if(use>0){
            return Result.ok("使用成功");
        }
        return Result.fail("使用失败");
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable("id") Long id) {
        Integer cancel = orderService.cancel(id);
        if(cancel>0){
            return Result.ok("取消成功");
        }
        return Result.fail("取消失败");
    }

    /**
     * 退款订单
     */
    @PostMapping("/refund/{id}")
    public Result refund(@PathVariable("id") Long id) {
        Integer refund = orderService.refund(id);
        if(refund>0){
            return Result.ok("退款成功");
        }
        return Result.fail("退款失败");
    }


    /**
     * 查询订单创建状态 (用于前端轮询)
     * @param id 订单ID
     * @return PENDING / SUCCESS / FAILED
     */
    @GetMapping("/status/{id}")
    public Result getOrderStatus(@PathVariable("id") Long id) {
        return Result.ok(orderService.getOrderStatus(id));
    }
}
