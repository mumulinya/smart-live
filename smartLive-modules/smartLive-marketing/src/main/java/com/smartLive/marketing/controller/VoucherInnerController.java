package com.smartLive.marketing.controller;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.marketing.domain.Voucher;
import com.smartLive.marketing.service.ISeckillVoucherService;
import com.smartLive.marketing.service.IVoucherService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券内部Controller
 * 
 * @author 木木林
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/voucher")
public class VoucherInnerController extends BaseController {
    @Autowired
    private IVoucherService voucherService;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    /**
     *   秒杀优惠券(ai代买)
     */
    @PostMapping("/orderSeckillVoucher")
    public Long orderSeckillVoucher(@RequestParam("id") Long voucherId, @RequestParam("userId") Long userId) {
        return voucherService.seckillVoucher(voucherId, userId);
    }
    /**
     * 购买优惠券(ai代买)
     */
    @PostMapping("/orderVoucher")
    public Long orderVoucher(@RequestParam("id") Long voucherId,@RequestParam("userId") Long userId) {
        return voucherService.buyVoucher(voucherId, userId);
    }

    /**
     * 更新优惠券库存
     *
     * @param voucherId 优惠券id
     * @return
     */
    @PostMapping("/{voucherId}")
    public Boolean updateSeckillVoucherByVoucherId(@PathVariable("voucherId") Long voucherId) {
        return seckillVoucherService.updateSeckillVoucherByVoucherId(voucherId);
    }
    /**
     * 恢复秒杀券库存
     */
    @PostMapping("/recover/{id}")
    public Boolean recoverVoucherStock(@PathVariable("id") Long voucherId){
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
    public Integer getCouponTotal(){
        return voucherService.getCouponTotal();
    }
    /**
     * 获取优惠券列表
     */
    @GetMapping("/getVoucherListByIds")
    public List<Voucher> getVoucherListByIds(@RequestParam("sourceIdList")List<Long> sourceIdList){
        return voucherService.getVoucherListByIds(sourceIdList);
    }
    @GetMapping("/{id}")
    public Voucher getVoucherById(@PathVariable("id") Long voucherId){
        return voucherService.selectVoucherById(voucherId);
    }
}
