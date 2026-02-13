package com.smartLive.interaction.controller.inner;

import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.service.IFollowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 关注服务内部接口
 */
@RestController
@RequestMapping("/inner/follow")
public class FollowInnerController {
    @Resource
    private IFollowService followServiceImpl;
    /**
     * 查询是否关注
     * @param follow 关注查询条件
     * @return 是否关注
     */
    @GetMapping("/isFollow")
    public Boolean isFollowed(Follow follow){
        return followServiceImpl.isFollowed(follow);
    }
    /**
     * 获取关注数
     * @param follow 关注查询条件
     * @return 关注数
     */
    @GetMapping("/getFollowCount")
    public Integer getFollowCount(Follow follow){
        return followServiceImpl.getFollowCount(follow);
    }
    /**
     * 获取粉丝数
     * @param follow 关注查询条件
     * @return 粉丝数
     */
    @GetMapping("/getFanCount")
    public Integer getFanCount(Follow follow){
        return followServiceImpl.getFanCount(follow);
    }
    /**
     * 获取共同关注数
     * @param follow 关注查询条件
     * @return 共同关注数
     */
    @GetMapping("/getCommonFollowCount")
    public Integer getCommonCount(Follow follow){
         return followServiceImpl.getCommonFollowCount(follow);
    }
}
