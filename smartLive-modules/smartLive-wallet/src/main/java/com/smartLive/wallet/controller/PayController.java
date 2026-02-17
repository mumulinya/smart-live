package com.smartLive.wallet.controller;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.domain.vo.PayStatusVO;
import com.smartLive.wallet.domain.vo.UnifiedPayVO;
import com.smartLive.wallet.service.IPayService;
import com.smartLive.wallet.service.impl.BalancePayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付Controller
 *
 * @author smartLive
 */
@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private IPayService payService;

    @Autowired
    private BalancePayService balancePayService;

    /**
     * 统一下单接口
     */
    @PostMapping("/unified")
    public Result unifiedOrder(@RequestBody UnifiedPayDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(payService.unifiedOrder(userId, dto));
    }

    /**
     * 余额支付接口
     */
    @PostMapping("/balance")
    public Result balancePay(@RequestBody UnifiedPayDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        balancePayService.pay(userId, dto);
        return Result.ok();
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/status")
    public Result queryPayStatus(@RequestParam("paySn") String paySn) {
        return Result.ok(payService.queryPayStatus(paySn));
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/callback/wechat")
    public void wechatCallback(HttpServletRequest request, HttpServletResponse response) {
        payService.handleWechatCallback(request, response);
    }

    /**
     * 支付宝支付回调
     */
    @PostMapping("/callback/alipay")
    public Result alipayCallback(HttpServletRequest request) {
        return Result.ok(payService.handleAlipayCallback(request));
    }

    /**
     * 查询支付记录列表
     */
    @GetMapping("/list")
    public Result getPayList(@RequestParam(value = "page",defaultValue = "1") Integer page,
                             @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize,
                             @RequestParam(value = "status",required = false) Integer status) {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(payService.getPayList(userId, page, pageSize, status));
    }

    /**
     * 取消支付
     */
    @PostMapping("/cancel")
    public Result cancelPay(@RequestBody Map<String, String> body) {
        String paySn = body.get("paySn");
        Long userId = UserContextHolder.getUser().getId();
        payService.cancelPay(userId, paySn);
        return Result.ok();
    }
}
