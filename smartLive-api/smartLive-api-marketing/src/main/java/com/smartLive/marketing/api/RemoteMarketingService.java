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

@FeignClient(contextId = "remoteMarketingService", value = ServiceNameConstants.MARKETING_SERVICE, fallbackFactory = RemoteMarketingFallbackFactory.class)
public interface RemoteMarketingService {
    /**
     * 更新优惠券表
     */
    @PostMapping("/voucher/{id}")
     R<Boolean> updateVoucherById(@PathVariable("id") Long voucherId);

    @GetMapping("/voucher/{id}")
    Result getVoucherById(@PathVariable("id") Long voucherId);
}
