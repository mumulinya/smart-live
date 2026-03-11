package com.smartLive.wallet.controller;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.wallet.domain.dto.WalletPasswordSetDTO;
import com.smartLive.wallet.domain.dto.WalletPasswordVerifyDTO;
import com.smartLive.wallet.domain.dto.WalletRechargeDTO;
import com.smartLive.wallet.domain.vo.WalletInfoVO;
import com.smartLive.wallet.service.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 个人钱包业务控制层
 * 处理个人余额查询、流水分析、模拟充值以及支付密码的设置与核验功能。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private IWalletService walletService;

    /**
     * 获取当前用户的钱包账户概览
     * 包含实时余额、累计收益/消费等基础统计。
     * 
     * @return 钱包详情 VO
     */
    @GetMapping("/info")
    public Result getWalletInfo() {
        Long userId = UserContextHolder.getUser().getId();
        WalletInfoVO data = walletService.getWalletInfo(userId);
        return Result.ok(data);
    }

    /**
     * 查询钱包收支明细记录列表
     * 
     * @param page 页码
     * @param pageSize 每页条数
     * @param type 类型过滤（income-收入，expense-支出，all-全部）
     * @return 事务列表 Result
     */
    @GetMapping("/transaction/list")
    public Result getTransactionList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "type", defaultValue = "all") String type) {
        Long userId = UserContextHolder.getUser().getId();
        Map<String, Object> data = walletService.getTransactionList(userId, page, pageSize, type);
        return Result.ok(data);
    }

    /**
     * 账户充值（模拟充值逻辑）
     * 
     * @param dto 充值参数
     * @return 最新余额 Result
     */
    @PostMapping("/recharge")
    public Result recharge(@RequestBody WalletRechargeDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        BigDecimal newBalance = walletService.recharge(userId, dto == null ? null : dto.getAmount());
        Map<String, Object> data = new HashMap<>();
        data.put("newBalance", newBalance);
        return Result.ok(data);
    }

    /**
     * 设置或重置钱包支付密码
     * 
     * @param dto 包含密码的参数包
     * @return 状态结果 Result
     */
    @PostMapping("/password/set")
    public Result setPayPassword(@RequestBody WalletPasswordSetDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        walletService.setPayPassword(userId, dto == null ? null : dto.getPassword());
        return Result.ok();
    }

    /**
     * 核验钱包支付密码是否正确
     * 
     * @param dto 待校验密码
     * @return 核验结果（verified: true/false）
     */
    @PostMapping("/password/verify")
    public Result verifyPayPassword(@RequestBody WalletPasswordVerifyDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        boolean verified = walletService.verifyPayPassword(userId, dto == null ? null : dto.getPassword());
        Map<String, Object> data = new HashMap<>();
        data.put("verified", verified);
        return Result.ok(data);
    }
}
