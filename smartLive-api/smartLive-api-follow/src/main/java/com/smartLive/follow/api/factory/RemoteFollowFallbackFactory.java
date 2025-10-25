package com.smartLive.follow.api.factory;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.domain.R;
import com.smartLive.follow.api.RemoteFollowService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteFollowFallbackFactory implements FallbackFactory<RemoteFollowService> {
    @Override
    public RemoteFollowService create(Throwable cause) {
        return null;
    }
}
