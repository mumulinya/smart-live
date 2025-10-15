package com.smartLive.user.controller;


import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.service.IFollowService;
import com.smartLive.user.service.IFollowShopService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/followShop")
public class FollowShopController {
    @Resource
    private IFollowShopService followShopService;

    @Resource
    private IFollowService followServiceImpl;
    /**
     * 关注或取关
     * @param
     * @param isFollow 是否关注
     * @return
     */
    @PutMapping("/{shopId}/{isFollow}")
    public Result follow(@PathVariable("shopId") Long shopId, @PathVariable("isFollow") Boolean isFollow) {
        return followShopService.follow(shopId, isFollow);
    }
    /**
     * 查询是否关注
     * @param followUserId 用户id
     * @return
     */
    @GetMapping("/isFollow/{id}")
    public Result isFollowed(@PathVariable("id") Long followUserId){
        return followShopService.isFollowed(followUserId);
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
        return followShopService.getFollowShops(userId, current);
    }
}
