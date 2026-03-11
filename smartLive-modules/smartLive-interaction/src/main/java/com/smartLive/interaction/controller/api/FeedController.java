package com.smartLive.interaction.controller.api;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.service.IFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * 社交动态（Feed）控制层
 * 实现基于 Timeline（时间线）的推拉结合模式，展示关注对象的博客更新动态。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/feed")
public class FeedController {
    @Autowired
    private IFeedService feedService;
    /**
     * 滚动分页获取用户动态（Feed 流）
     * 使用最后一条记录的 ID 和偏移量来实现流式加载，避免传统分页在大数据量下的性能及重复显示问题。
     *
     * @param feedType 动态分类（0: 全部）
     * @param lastId   上一次请求最后一条动态的 ID（用于分页定位）
     * @param offset   偏移量（本次查询相对于 lastId 的偏移）
     * @return 封装后的动态列表（带有下一次请求所需的 lastId 和 offset）
     */
    @GetMapping
    public Result getFeedList(@RequestParam(value = "feedType",defaultValue = "0") Integer feedType,@RequestParam(value = "lastId") Long lastId, @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return Result.ok(feedService.queryFeedList(feedType,lastId, offset));
    }
}
