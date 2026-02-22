package com.smartLive.interaction.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.factory.RemoteSyncFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
    contextId = "remoteSyncService",
    value = ServiceNameConstants.INTERACTION_SERVICE,
    fallbackFactory = RemoteSyncFallbackFactory.class
)
public interface RemoteSyncService {
    @PostMapping("/inner/sync/trigger")
    Boolean trigger();
}
