package com.smartLive.wallet.controller;

import com.smartLive.wallet.service.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 钱包内部接口控制器。
 * 供订单模块同步发起余额退款请求。
 */
@RestController
@RequestMapping("/inner/wallet")
public class InnerWalletController {

    @Autowired
    private IWalletService walletService;

    /**
     * 同步执行余额退款。
     * 该接口只处理平台钱包余额场景，不处理第三方原路退款。
     */
    @PutMapping("/refund/balance/{orderId}")
    public Boolean refundOrderBalance(@PathVariable("orderId") Long orderId,
                                      @RequestParam("userId") Long userId,
                                      @RequestParam("amount") BigDecimal amount) {
        walletService.refundOrder(userId, amount, String.valueOf(orderId));
        return Boolean.TRUE;
    }
}
