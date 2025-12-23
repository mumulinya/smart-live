package com.smartLive.interaction.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.service.ILikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/like")
public class LikeController {
    @Autowired
    private ILikeService likeRecordService;

    /**
     * 点赞或取消点赞
     * @param like
     * @return
     */
    @PutMapping()
    public Result likeBlog(@RequestBody Like like) {
        return Result.ok(likeRecordService.likeOrCancelLike(like));
    }
    /**
     * 查询是否点赞
     * @param
     * @return
     */
    @GetMapping("/isLike")
    public Result isLike(Like like) {
        return Result.ok(likeRecordService.isLike(like));
    }
    /**
     * 查询点赞数
     * @param
     * @return
     */
    @GetMapping("/likeCount")
    public Result queryBlogLikes(Like like) {
        return Result.ok(likeRecordService.queryLikeCount(like));
    }
    /**
     * 查询点赞记录
     */
    @GetMapping("/likeRecord")
    public Result queryLikeRecord(Like like, Integer current) {
        return Result.ok(likeRecordService.queryLikeRecord(like, current));
    }
    /**
     * 查询点赞用户列表
     */
    @GetMapping("/likeUserList")
    public Result queryLikeUserList(Like like) {
        return Result.ok(likeRecordService.queryLikeUserList(like));
    }
}
