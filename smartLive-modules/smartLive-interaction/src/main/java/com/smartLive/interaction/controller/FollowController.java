package com.smartLive.interaction.controller;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.service.IFollowService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;

/**
 * 关注Controller
 *
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followServiceImpl;
    /**
     * 关注或取关
     * @param
     * @param
     * @return
     */
    @PutMapping()
    public Result follow(@RequestBody Follow follow) {
        Boolean f = followServiceImpl.follow(follow);
        if (f) {
            return Result.ok("操作成功");
        }
        return Result.fail("操作失败");
    }
    /**
     * 查询是否关注
     * @param follow
     * @return
     */
    @GetMapping("/isFollow")
    public Result isFollowed(Follow follow){
        return Result.ok(followServiceImpl.isFollowed(follow));
    }
    /**
     * 查询共同关注用户列表
     * @param follow
     * @return
     */
    @GetMapping("/common")
    public Result common(Follow follow,@RequestParam("current") Integer current){
        return Result.ok(followServiceImpl.common(follow,current));
    }

    //获取粉丝列表
    @GetMapping("/fans")
    public Result getFans(Follow follow,@RequestParam("current") Integer current){
        return Result.ok(followServiceImpl.getFans(follow,current));
    }
    //获取关注列表
    @GetMapping("/follows")
    public Result getFollows(Follow follow,@RequestParam("current") Integer current){
        return Result.ok(followServiceImpl.getFollows(follow,current));
    }
    /**
     * 获取关注数
     * @param follow
     * @return
     */
    @GetMapping("/getFollowCount")
    public Result getFollowCount(Follow follow){
        Integer followCount =followServiceImpl.getFollowCount(follow);
        return Result.ok(followCount);
    }
    /**
     * 获取粉丝数
     * @param follow
     * @return
     */
    @GetMapping("/getFanCount")
    public Result getFanCount(Follow follow){
        Integer fansCount =followServiceImpl.getFanCount(follow);
        return Result.ok(fansCount);
    }
    /**
     * 获取共同关注数
      * @param follow
     * @return
     */
    @GetMapping("/getCommonFollowCount")
    public Result getCommonCount(Follow follow){
         Integer commonCount =followServiceImpl.getCommonFollowCount(follow);
         return Result.ok(commonCount);
    }
}
