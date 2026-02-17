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
 * Wallet app controller.
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private IWalletService walletService;

    @GetMapping("/info")
    public Result getWalletInfo() {
        Long userId = UserContextHolder.getUser().getId();
        WalletInfoVO data = walletService.getWalletInfo(userId);
        return Result.ok(data);
    }

    @GetMapping("/transaction/list")
    public Result getTransactionList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "type", defaultValue = "all") String type) {
        Long userId = UserContextHolder.getUser().getId();
        Map<String, Object> data = walletService.getTransactionList(userId, page, pageSize, type);
        return Result.ok(data);
    }

    @PostMapping("/recharge")
    public Result recharge(@RequestBody WalletRechargeDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        BigDecimal newBalance = walletService.recharge(userId, dto == null ? null : dto.getAmount());
        Map<String, Object> data = new HashMap<>();
        data.put("newBalance", newBalance);
        return Result.ok(data);
    }

    @PostMapping("/password/set")
    public Result setPayPassword(@RequestBody WalletPasswordSetDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        walletService.setPayPassword(userId, dto == null ? null : dto.getPassword());
        return Result.ok();
    }

    @PostMapping("/password/verify")
    public Result verifyPayPassword(@RequestBody WalletPasswordVerifyDTO dto) {
        Long userId = UserContextHolder.getUser().getId();
        boolean verified = walletService.verifyPayPassword(userId, dto == null ? null : dto.getPassword());
        Map<String, Object> data = new HashMap<>();
        data.put("verified", verified);
        return Result.ok(data);
    }
}
