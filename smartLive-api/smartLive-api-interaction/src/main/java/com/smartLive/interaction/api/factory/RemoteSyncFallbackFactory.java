package com.smartLive.interaction.api.factory;

import com.smartLive.interaction.api.RemoteSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RemoteSyncFallbackFactory implements FallbackFactory<RemoteSyncService> {
    @Override
    public RemoteSyncService create(Throwable throwable) {
        return new RemoteSyncService() {
            @Override
            public Boolean trigger() {
                log.error("trigger interaction sync failed: {}", throwable.getMessage());
                return false;
            }
        };
    }
}

