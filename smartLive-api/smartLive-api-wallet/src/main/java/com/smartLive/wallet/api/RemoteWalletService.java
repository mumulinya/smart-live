package com.smartLive.wallet.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.wallet.api.factory.RemoteWalletFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 钱包内部 RPC 接口。
 * 目前主要供订单模块在余额退款场景下同步调用。
 */
@FeignClient(contextId = "remoteWalletService", value = ServiceNameConstants.WALLET_SERVICE, fallbackFactory = RemoteWalletFallbackFactory.class)
public interface RemoteWalletService {

    /**
     * 同步执行余额退款。
     *
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param amount 退款金额
     * @return 是否退款成功
     */
    @PutMapping("/inner/wallet/refund/balance/{orderId}")
    Boolean refundOrderBalance(@PathVariable("orderId") Long orderId,
                               @RequestParam("userId") Long userId,
                               @RequestParam("amount") BigDecimal amount);
}
