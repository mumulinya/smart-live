package com.smartLive.interaction.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.service.ILikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;
/**
 * 点赞服务内部接口
 */
@RestController
@RequestMapping("/inner/like")
public class LikeInnerController {
    @Autowired
    private ILikeService likeRecordService;
    /**
     * 查询是否点赞
     * @param
     * @return
     */
    @GetMapping("/isLike")
    public Boolean  isLike (Like like) {
        return  likeRecordService.isLike(like);
    }
    /**
     * 查询点赞数
     * @param
     * @return
     */
    @GetMapping("/getLikeCount")
    public Integer queryBlogLikes(Like like) {
        return likeRecordService.queryLikeCount(like);
    }
    /**
     * 获取用户点赞数
     * @return
     */
    @GetMapping("/getUserLikeCount")
    Integer getUserLikeCount(@SpringQueryMap Like like){
        return likeRecordService.getUserLikeCount(like);
    }
}
