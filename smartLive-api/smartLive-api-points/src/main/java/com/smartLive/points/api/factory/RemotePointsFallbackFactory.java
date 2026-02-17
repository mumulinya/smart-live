package com.smartLive.points.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.points.api.RemotePointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RemotePointsFallbackFactory implements FallbackFactory<RemotePointsService> {
    @Override
    public RemotePointsService create(Throwable cause) {
        return new RemotePointsService() {
            @Override
            public Boolean addPoints(Long userId, Integer amount, String bizId, String description) {
                log.error("新增积分失败:{}", cause.getMessage());
                return false;
            }
        };
    }
}
