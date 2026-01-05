package com.smartLive.interaction.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.api.dto.FeedEventDTO;
import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.service.IFollowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 关注服务内部调用
 */
@RestController
@RequestMapping("/inner/follow")
public class FollowInnerController {
    @Resource
    private IFollowService followServiceImpl;
    /**
     * 查询是否关注
     * @param follow
     * @return
     */
    @GetMapping("/isFollow")
    public Boolean isFollowed(Follow follow){
        return followServiceImpl.isFollowed(follow);
    }
    /**
     * 获取关注数
     * @param follow
     * @return
     */
    @GetMapping("/getFollowCount")
    public Integer getFollowCount(Follow follow){
        return followServiceImpl.getFollowCount(follow);
    }
    /**
     * 获取粉丝数
     * @param follow
     * @return
     */
    @GetMapping("/getFanCount")
    public Integer getFanCount(Follow follow){
        return followServiceImpl.getFanCount(follow);
    }
    /**
     * 获取共同关注数
      * @param follow
     * @return
     */
    @GetMapping("/getCommonFollowCount")
    public Integer getCommonCount(Follow follow){
         return followServiceImpl.getCommonFollowCount(follow);
    }
}
