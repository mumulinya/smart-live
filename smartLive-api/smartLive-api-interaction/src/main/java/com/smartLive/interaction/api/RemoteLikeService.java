package com.smartLive.interaction.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.interaction.api.dto.LikeDTO;
import com.smartLive.interaction.api.factory.RemoteLikeFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;



@FeignClient(contextId = "remoteLikeService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteLikeFallbackFactory.class)
public interface RemoteLikeService {
    /**
     * 查询是否关注
     * @param
     * @return
     */
    @GetMapping("/like/isLike")
    R<Boolean> isLike(@SpringQueryMap LikeDTO likeDTO);
    /**
     * 获取关注数
     * @return
     */
    @GetMapping("/like/getLikeCount")
     R<Integer> getLikeCount(@SpringQueryMap LikeDTO likeDTO);
    /**
     * 获取粉丝数
     * @return
     */
    @GetMapping("/like/getFanCount")
     R<Integer> getFanCount(@SpringQueryMap LikeDTO likeDTO);

    /**
     * 获取共同关注数
     * @return
     */
    @GetMapping("/like/getCommonLikeCount")
     R<Integer> getCommonLikeCount(@SpringQueryMap LikeDTO likeDTO);

    /**
     * 获取用户关注店铺数量
     * @return
     */
    @GetMapping("/like/getLikeShopCount/{id}")
     R<Integer> getLikeShopCount(@SpringQueryMap LikeDTO likeDTO);
}
