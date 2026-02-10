package com.smartLive.interaction.api.factory;

import com.smartLive.interaction.api.RemoteReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RemoteReviewFallbackFactory implements FallbackFactory<RemoteReviewService> {
    @Override
    public RemoteReviewService create(Throwable throwable) {
        return new RemoteReviewService() {
            @Override
            public Boolean updateReviewStatus(Long id, Integer status) {
                log.error("评价服务调用失败:{}", throwable.getMessage());
                return false;
            }
        };
    }
}
