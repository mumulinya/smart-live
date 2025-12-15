package com.smartLive.marketing.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.api.dto.VoucherDTO;
import com.smartLive.marketing.api.factory.RemoteMarketingFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "remoteMarketingService", value = ServiceNameConstants.MARKETING_SERVICE, fallbackFactory = RemoteMarketingFallbackFactory.class)
public interface RemoteMarketingService {
    /**
     * 更新优惠券表
     */
    @PostMapping("/voucher/{id}")
     R<Boolean> updateVoucherById(@PathVariable("id") Long voucherId);

    @GetMapping("/voucher/{id}")
    R<VoucherDTO> getVoucherById(@PathVariable("id") Long voucherId);
    /**
     * 恢复库存
     */
    @PostMapping("/voucher/recover/{id}")
    R<Boolean> recoverVoucherStock(@PathVariable("id") Long voucherId);
    /**
     * 获取代金券总数
     */
    @GetMapping("/voucher/total")
    R<Integer> getCouponTotal();
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
