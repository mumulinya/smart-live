package com.smartLive.interaction.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.dto.FollowDTO;
import com.smartLive.interaction.api.factory.RemoteFollowFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(contextId = "remoteFollowService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteFollowFallbackFactory.class)
public interface RemoteFollowService {
    /**
     * 查询是否关注
     * @param
     * @return
     */
    @GetMapping("/inner/follow/isFollow")
    Boolean isFollowed(@SpringQueryMap FollowDTO followDTO);
    /**
     * 获取关注数
     * @return
     */
    @GetMapping("/inner/follow/getFollowCount")
     Integer getFollowCount(@SpringQueryMap FollowDTO followDTO);
    /**
     * 获取粉丝数
     * @return
     */
    @GetMapping("/inner/follow/getFanCount")
     Integer getFanCount(@SpringQueryMap FollowDTO followDTO);

    /**
     * 获取共同关注数
     * @return
     */
    @GetMapping("/inner/follow/getCommonFollowCount")
     Integer getCommonFollowCount(@SpringQueryMap FollowDTO followDTO);
}
