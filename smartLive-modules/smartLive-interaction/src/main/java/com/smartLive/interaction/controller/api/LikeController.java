package com.smartLive.interaction.controller.api;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.service.ILikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.smartLive.interaction.api.DTO.LikeDTO;


/**
 * 点赞管理控制层
 * 支持对博客、评价、评论等全站内容进行点赞/取消点赞操作，提供点赞数统计及记录查询功能。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/like")
public class LikeController {
    @Autowired
    private ILikeService likeRecordService;

    /**
     * 点赞或取消点赞
     *
     * @param like 点赞实体
     * @return 操作结果
     */
    @PutMapping()
    public Result likeOrCancelLike(@RequestBody Like like) {
        return Result.ok(likeRecordService.likeOrCancelLike(like));
    }

    /**
     * 查询点赞数
     *
     * @param like 点赞查询条件
     * @return 点赞数
     */
    @GetMapping("/likeCount")
    public Result queryBlogLikes(Like like) {
        return Result.ok(likeRecordService.queryLikeCount(like));
    }

    /**
     * 查询点赞记录
     *
     * @param like    点赞查询条件
     * @param current 当前页码
     * @return 点赞记录
     */
    @GetMapping("/likeRecord")
    public Result queryLikeRecord(Like like, @RequestParam("current") Integer current) {
        return Result.ok(likeRecordService.queryLikeRecord(like, current));
    }

    /**
     * 查询用户点赞记录（与评论/评价模块保持一致的路由）
     *
     * @param like    点赞查询条件
     * @param current 当前页
     * @return 点赞记录
     */
    @GetMapping("/of/user")
    public Result getLikeOfUser(Like like, @RequestParam("current") Integer current) {
        return Result.ok(likeRecordService.queryLikeRecord(like, current));
    }

    /**
     * 查询点赞用户列表
     *
     * @param like 点赞查询条件
     * @return 点赞用户列表
     */
    @GetMapping("/likeUserList")
    public Result queryLikeUserList(Like like) {
        return Result.ok(likeRecordService.queryLikeUserList(like));
    }

    /**
     * 批量查询资源是否已点赞
     *
     * @param likeDTO   业务类型信息
     * @param sourceIds 待查询的资源 ID 列表
     * @return ID 与点赞状态的映射结果
     */
    @GetMapping("/getIsLikeBatch")
    public Result getIsLikeBatch(LikeDTO likeDTO, @RequestParam("sourceIds") List<Long> sourceIds) {
        return Result.ok(likeRecordService.isLikeBatch(likeDTO, sourceIds));
    }
}