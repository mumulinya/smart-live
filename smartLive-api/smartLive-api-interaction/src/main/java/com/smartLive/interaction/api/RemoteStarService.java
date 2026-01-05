package com.smartLive.interaction.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.interaction.api.dto.StarDTO;
import com.smartLive.interaction.api.factory.RemoteStarFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;


@FeignClient(contextId = "remoteStarService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteStarFallbackFactory.class)
public interface RemoteStarService {
    /**
     * 查询是否收藏
     * @param
     * @return
     */
    @GetMapping("/inner/star/isStar")
    Boolean isStared(@SpringQueryMap StarDTO starDTO);
    /**
     * 获取收藏数
     * @return
     */
    @GetMapping("/inner/star/getStarCount")
     Integer getStarCount(@SpringQueryMap StarDTO starDTO);
    /**
     * 获取粉丝数
     * @return
     */
    @GetMapping("/inner/star/getFanCount")
     Integer getFanCount(@SpringQueryMap StarDTO starDTO);

    /**
     * 获取共同收藏数
     * @return
     */
    @GetMapping("/inner/star/getCommonStarCount")
     Integer getCommonStarCount(@SpringQueryMap StarDTO starDTO);

    /**
     * 获取用户关注店铺数量
     * @return
     */
    @GetMapping("/inner/star/getStarShopCount")
     Integer getStarShopCount(@SpringQueryMap StarDTO starDTO);
}
