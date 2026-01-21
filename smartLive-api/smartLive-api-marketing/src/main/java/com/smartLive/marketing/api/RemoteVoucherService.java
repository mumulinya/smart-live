package com.smartLive.marketing.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.marketing.api.DTO.VoucherDTO;
import com.smartLive.marketing.api.factory.RemoteVoucherFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(contextId = "remoteVoucherService", value = ServiceNameConstants.MARKETING_SERVICE, fallbackFactory = RemoteVoucherFallbackFactory.class)
public interface RemoteVoucherService {
    /**
     * 更新优惠券
     */
    @PostMapping("/inner/voucher/{id}")
     Boolean updateVoucherById(@PathVariable("id") Long voucherId);

    @GetMapping("/inner/voucher/{id}")
    VoucherDTO getVoucherById(@PathVariable("id") Long voucherId);
    /**
     * 恢复库存
     */
    @PostMapping("/inner/voucher/recover/{id}")
    Boolean recoverVoucherStock(@PathVariable("id") Long voucherId);
    /**
     * 获取代金券总数
     */
    @GetMapping("/inner/voucher/total")
    Integer getCouponTotal();
    /**
     * 秒杀优惠券
     */
    @PostMapping("/inner/voucher/orderSeckillVoucher")
    Long seckillVoucher(@RequestParam("id") Long voucherId, @RequestParam("userId") Long userId);

    /**
     * 购买优惠券
     */
    @PostMapping("/inner/voucher/orderVoucher")
    Long buyVoucher(@RequestParam("id") Long voucherId, @RequestParam("userId") Long userId);
    /**
     * 获取优惠券列表
     */
    @GetMapping("/inner/voucher/getVoucherListByIds")
    List<VoucherDTO> getVoucherListByIds(@RequestParam("sourceIdList")List<Long> sourceIdList);
}
