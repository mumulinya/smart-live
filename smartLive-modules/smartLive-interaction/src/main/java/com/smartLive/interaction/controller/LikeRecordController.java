package com.smartLive.interaction.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.LikeRecord;
import com.smartLive.interaction.service.ILikeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/likeRecord")
public class LikeRecordController {
    @Autowired
    private ILikeRecordService likeRecordService;

    /**
     * 点赞或取消点赞
     * @param likeRecord
     * @return
     */
    @PutMapping("/like")
    public Result likeBlog(LikeRecord likeRecord) {
        return Result.ok(likeRecordService.likeOrCancelLike(likeRecord));
    }
    /**
     * 查询点赞数
     * @param
     * @return
     */
    @GetMapping("/likeCount")
    public Result queryBlogLikes(LikeRecord likeRecord) {
        return Result.ok(likeRecordService.queryLikeCount(likeRecord));
    }
    /**
     * 查询点赞记录
     */
    @GetMapping("/likeRecord")
    public Result queryLikeRecord(LikeRecord likeRecord, Integer current) {
        return Result.ok(likeRecordService.queryLikeRecord(likeRecord, current));
    }
    /**
     * 查询点赞用户列表
     */
    @GetMapping("/likeUserList")
    public Result queryLikeUserList(LikeRecord likeRecord) {
        return Result.ok(likeRecordService.queryLikeUserList(likeRecord));
    }
}
