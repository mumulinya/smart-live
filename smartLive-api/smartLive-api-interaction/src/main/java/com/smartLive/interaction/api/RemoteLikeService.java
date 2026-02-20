package com.smartLive.interaction.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.api.factory.RemoteLikeFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;



@FeignClient(contextId = "remoteLikeService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteLikeFallbackFactory.class)
public interface RemoteLikeService {
    /**
     * 查询是否点赞
     * @param
     * @return
     */
    @GetMapping("/inner/like/isLike")
    Boolean isLike(@SpringQueryMap LikeDTO likeDTO);
    /**
     * 获取点赞数
     * @return
     */
    @GetMapping("/inner/like/getLikeCount")
     Integer getLikeCount(@SpringQueryMap LikeDTO likeDTO);

    /**
     * 获取共同点赞数
     * @return
     */
    @GetMapping("/inner/like/getCommonLikeCount")
     Integer getCommonLikeCount(@SpringQueryMap LikeDTO likeDTO);
    /**
     * 获取用户点赞数
     * @return
     */
    @GetMapping("/inner/like/getUserLikeCount")
     Integer getUserLikeCount(@SpringQueryMap LikeDTO likeDTO);

    /**
     * 批量查询是否点赞
     * @param likeDTO
     * @param sourceIds
     * @return
     */
    @GetMapping("/inner/like/getIsLikeBatch")
    Map<Long, Boolean> getIsLikeBatch(@SpringQueryMap LikeDTO likeDTO, @RequestParam("sourceIds") List<Long> sourceIds);
}
