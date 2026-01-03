package com.smartLive.interaction.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.api.dto.FollowDTO;
import com.smartLive.interaction.api.factory.RemoteFollowFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(contextId = "remoteFollowService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteFollowFallbackFactory.class)
public interface RemoteFollowService {
    /**
     * 查询是否关注
     * @param
     * @return
     */
    @GetMapping("/follow/isFollow")
    R<Boolean> isFollowed(@SpringQueryMap FollowDTO followDTO);
    /**
     * 获取关注数
     * @return
     */
    @GetMapping("/follow/getFollowCount")
     R<Integer> getFollowCount(@SpringQueryMap FollowDTO followDTO);
    /**
     * 获取粉丝数
     * @return
     */
    @GetMapping("/follow/getFanCount")
     R<Integer> getFanCount(@SpringQueryMap FollowDTO followDTO);

    /**
     * 获取共同关注数
     * @return
     */
    @GetMapping("/follow/getCommonFollowCount")
     R<Integer> getCommonFollowCount(@SpringQueryMap FollowDTO followDTO);

    /**
     * 获取用户关注店铺数量
     * @return
     */
    @GetMapping("/follow/getFollowShopCount")
     R<Integer> getFollowShopCount(@SpringQueryMap FollowDTO followDTO);
}
