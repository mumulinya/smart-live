package com.smartLive.ai.api.factory;

import com.smartLive.ai.api.RemoteAiMerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RemoteAiMerchantFallbackFactory implements FallbackFactory<RemoteAiMerchantService> {

    @Override
    public RemoteAiMerchantService create(Throwable cause) {
        return new RemoteAiMerchantService() {
            @Override
            public List<String> extractKeywords(List<String> contents) {
                log.error("extract merchant keywords failed: {}", cause.getMessage());
                return new ArrayList<>();
            }
        };
    }
}
