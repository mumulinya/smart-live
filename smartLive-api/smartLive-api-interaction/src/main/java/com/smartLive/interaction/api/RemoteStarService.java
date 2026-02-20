package com.smartLive.interaction.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.interaction.api.factory.RemoteStarFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;


@FeignClient(contextId = "remoteStarService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteStarFallbackFactory.class)
public interface RemoteStarService {
    /**
     * 查询是否收藏
     * @param
     * @return
     */
    @GetMapping("/inner/star/isStar")
    Boolean isStar(@SpringQueryMap StarDTO starDTO);
    /**
     * 获取用户收藏数量
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
     * 获取用户收藏数量
     * @return
     */
    @GetMapping("/inner/star/getUserStarCount")
     Integer getUserStarCount(@SpringQueryMap StarDTO starDTO);

    /**
     * 批量查询是否收藏
     * @param starDTO
     * @param sourceIds
     * @return
     */
    @GetMapping("/inner/star/getIsStarBatch")
    Map<Long, Boolean> getIsStarBatch(@SpringQueryMap StarDTO starDTO, @RequestParam("sourceIds") List<Long> sourceIds);

}
