package com.smartLive.interaction.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.api.factory.RemoteFollowFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(contextId = "remoteInteractionService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteFollowFallbackFactory.class)
public interface RemoteFollowService {
    /**
     * 查询是否关注
     * @param followUserId 关注用户id
     * @return
     */
    @GetMapping("/followUser/isFollow/{id}")
     Result isFollowed(@PathVariable("id") Long followUserId);
    /**
     * 获取关注数
     * @param userId 用户id
     * @return
     */
    @GetMapping("/followUser/getFollowCount/{id}")
     Result getFollowCount(@PathVariable("id") Long userId);
    /**
     * 获取粉丝数
     * @param userId 用户id
     * @return
     */
    @GetMapping("/followUser/getFanCount/{id}")
     Result getFanCount(@PathVariable("id") Long userId);

    /**
     * 获取共同关注数
     * @param userId 用户id, currentUserId 当前用户id
     * @return
     */
    @GetMapping("/followUser/getCommonFollowCount")
     Result getCommonFollowCount(@RequestParam("userId") Long userId, @RequestParam("currentUserId") Long currentUserId);

    /**
     * 获取用户关注店铺数量
     * @param userId 用户id
     * @return
     */
    @GetMapping("/followShop/getFollowShopCount/{id}")
     Result getFollowShopCount(@PathVariable("id") Long userId);
}
