package com.smartLive.user.controller;


import com.smartLive.blog.domain.Blog;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.service.IFollowService;
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
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followServiceImpl;
    /**
     * 关注或取关
     * @param
     * @param isFollow 是否关注
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followServiceImpl.follow(followUserId, isFollow);
    }
    /**
     * 查询是否关注
     * @param followUserId 用户id
     * @return
     */
    @GetMapping("/isFollow/{id}")
    public Result isFollowed(@PathVariable("id") Long followUserId){
        return followServiceImpl.isFollowed(followUserId);
    }
    /**
     * 查询共同关注
     * @param userId 用户id
     * @return
     */
    @GetMapping("/common/{id}")
    public Result common(@PathVariable("id") Long userId){
        return followServiceImpl.common(userId);
    }

    //把博客笔记推送给所有粉丝
    @PostMapping("/send/blog")
    public void sendBlogToFollowers(@RequestBody BlogDTO blogDTO){
        followServiceImpl.sendBlogToFollowers(blogDTO);
    }
}
