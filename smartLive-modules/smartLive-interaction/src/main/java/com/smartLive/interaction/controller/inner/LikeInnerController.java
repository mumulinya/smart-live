package com.smartLive.interaction.controller.inner;

import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.service.ILikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;
/**
 * 点赞服务内部接口
 * 用于微服务间内部调用
 */
@RestController
@RequestMapping("/inner/like")
public class LikeInnerController {
    @Autowired
    private ILikeService likeRecordService;
    /**
     * 查询是否点赞
     * @param like 点赞查询条件
     * @return 是否点赞
     */
    @GetMapping("/isLike")
    public Boolean  isLike (Like like) {
        return  likeRecordService.isLike(like);
    }
    /**
     * 查询点赞数
     * @param like 点赞查询条件
     * @return 点赞数
     */
    @GetMapping("/getLikeCount")
    public Integer queryBlogLikes(Like like) {
        return likeRecordService.queryLikeCount(like);
    }
    /**
     * 获取用户点赞数
     * @param like 点赞查询条件
     * @return 用户点赞数
     */
    @GetMapping("/getUserLikeCount")
    Integer getUserLikeCount(@SpringQueryMap Like like){
        return likeRecordService.getUserLikeCount(like);
    }
}
