package com.smartLive.ai.feign;

import com.smartLive.common.core.web.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "smartLive-marketing")
public interface VoucherClient {
    /**
     * 秒杀优惠券
     */
    @PostMapping("/voucher/orderSeckillVoucher")
    Result seckillVoucher(@RequestParam("id") Long voucherId, @RequestParam("userId") Long userId);

    /**
     * 购买优惠券
     */
    @PostMapping("/voucher/orderVoucher")
    Result buyVoucher(@RequestParam("id") Long voucherId, @RequestParam("userId") Long userId);
}
