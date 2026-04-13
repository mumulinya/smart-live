package com.smartLive.wallet.api.factory;

import com.smartLive.wallet.api.RemoteWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 钱包内部 RPC 降级工厂。
 */
@Component
@Slf4j
public class RemoteWalletFallbackFactory implements FallbackFactory<RemoteWalletService> {
    @Override
    public RemoteWalletService create(Throwable cause) {
        return new RemoteWalletService() {
            @Override
            public Boolean refundOrderBalance(Long orderId, Long userId, BigDecimal amount) {
                log.error("refund balance order failed: {}", cause.getMessage());
                return Boolean.FALSE;
            }
        };
    }
}
