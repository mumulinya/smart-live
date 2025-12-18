package com.smartLive.interaction.controller;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.service.ICollectionShopService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/**
 * 收藏Controller
 *
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/collection")
public class CollectionController {
    @Resource
    private ICollectionShopService collectionShopService;
    /**
     * 关注或取关
     * @param
     * @param isFollow 是否关注
     * @return
     */
    @PutMapping("/{shopId}/{isFollow}")
    public Result follow(@PathVariable("shopId") Long shopId, @PathVariable("isFollow") Boolean isFollow) {
        return collectionShopService.follow(shopId, isFollow);
    }
    /**
     * 查询是否关注
     * @param followUserId 用户id
     * @return
     */
    @GetMapping("/isFollow/{id}")
    public Result isFollowed(@PathVariable("id") Long followUserId){
        return collectionShopService.isCollection(followUserId);
    }

    //获取粉丝列表
    @GetMapping("/fans")
    public Result getFans(){
        Long followUserId = UserContextHolder.getUser().getId();
        return null;
    }
    //获取关注店铺列表
    @GetMapping("/followShops")
    public Result getFollows(@RequestParam("userId") Long userId,@RequestParam("current") Integer current){
        return collectionShopService.getCollectionShops(userId, current);
    }
    /**
     * 获取用户关注店铺数量
     * @param userId 用户id
     * @return
     */
    @GetMapping("/getFollowShopCount/{id}")
    Result getFollowShopCount(@PathVariable("id") Long userId){
        Integer followShopCount=collectionShopService.getCollectCount(userId);
        return Result.ok(followShopCount);
    }
}
