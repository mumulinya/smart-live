package com.smartLive.follow.api;

import com.smartLive.blog.api.dto.BlogDto;


import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.follow.api.factory.RemoteFollowFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@FeignClient(contextId = "remoteFollowService", value = ServiceNameConstants.FOLLOW_SERVICE, fallbackFactory = RemoteFollowFallbackFactory.class)
public interface RemoteFollowService {
    /**
     * 查询是否关注
     * @param followUserId 用户id
     * @return
     */
    @GetMapping("/followUser/isFollow/{id}")
    public Result isFollowed(@PathVariable("id") Long followUserId);
}
