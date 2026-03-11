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
 * 支付控制层
 * 提供支付下单、余额扣款、支付状态查询以及第三方支付（微信、支付宝）回调处理。
 * 
 * @author smartLive
 * @date 2026-03-11
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
     * 支持多种支付渠道路由，返回包含支付跳转信息或参数包的统一结构。
     * 
     * @param dto 下单输入参数
     * @return 支付指令对象 Result
     */
    @PostMapping("/unified")
    public Result unifiedOrder(@RequestBody UnifiedPayDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(payService.unifiedOrder(userId, dto));
    }

    /**
     * 余额支付接口
     * 直接扣除用户钱包内的可用余额进行消费。
     * 
     * @param dto 支付参数
     * @return 状态结果 Result
     */
    @PostMapping("/balance")
    public Result balancePay(@RequestBody UnifiedPayDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        balancePayService.pay(userId, dto);
        return Result.ok();
    }

    /**
     * 轮询查询支付状态
     * 供前端在支付跳转后循环调用，获取该笔流水的最新的业务支付结果。
     * 
     * @param paySn 支付流水号
     * @return 支付详情 Result
     */
    @GetMapping("/status")
    public Result queryPayStatus(@RequestParam("paySn") String paySn) {
        return Result.ok(payService.queryPayStatus(paySn));
    }

    /**
     * 微信支付回调
     * 接收微信支付结果通知并处理业务入账逻辑。
     */
    @PostMapping("/callback/wechat")
    public void wechatCallback(HttpServletRequest request, HttpServletResponse response) {
        payService.handleWechatCallback(request, response);
    }

    /**
     * 支付宝支付回调
     * 接收支付宝支付结果通知。
     */
    @PostMapping("/callback/alipay")
    public Result alipayCallback(HttpServletRequest request) {
        return Result.ok(payService.handleAlipayCallback(request));
    }

    /**
     * 查询当前用户的支付流水记录列表
     * 包含支付中、成功、失败等各种状态。
     * 
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 过滤状态
     * @return 分页列表 Result
     */
    @GetMapping("/list")
    public Result getPayList(@RequestParam(value = "page",defaultValue = "1") Integer page,
                             @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize,
                             @RequestParam(value = "status",required = false) Integer status) {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(payService.getPayList(userId, page, pageSize, status));
    }

    /**
     * 取消进行中的支付
     * 
     * @param body 包含 paySn 的参数包
     * @return 结果 Result
     */
    @PostMapping("/cancel")
    public Result cancelPay(@RequestBody Map<String, String> body) {
        String paySn = body.get("paySn");
        Long userId = UserContextHolder.getUser().getId();
        payService.cancelPay(userId, paySn);
        return Result.ok();
    }
}
