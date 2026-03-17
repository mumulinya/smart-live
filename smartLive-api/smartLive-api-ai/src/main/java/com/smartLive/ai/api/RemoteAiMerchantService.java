package com.smartLive.ai.api;

import com.smartLive.ai.api.factory.RemoteAiMerchantFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        contextId = "remoteAiMerchantService",
        value = ServiceNameConstants.AI_SERVICE,
        fallbackFactory = RemoteAiMerchantFallbackFactory.class
)
public interface RemoteAiMerchantService {

    @PostMapping("/merchant/keywords")
    List<String> extractKeywords(@RequestBody List<String> contents);
}
