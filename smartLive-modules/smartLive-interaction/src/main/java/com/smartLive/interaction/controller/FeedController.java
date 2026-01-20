package com.smartLive.interaction.controller;

import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.service.IFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * @Description: 动态控制器
 * @Author: mumulin
 * @Date: 2026/1/21
 **/
@RestController
@RequestMapping("/feed")
public class FeedController {
    @Autowired
    private IFeedService feedService;
    /**
     * 分页获取用户动态列表
      * @param lastId
     * @param offset
     * @return
     */
    @GetMapping
    public Result getFeedList(@RequestParam(value = "feedType",defaultValue = "0") Integer feedType,@RequestParam(value = "lastId") Long lastId, @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return Result.ok(feedService.queryFeedList(feedType,lastId, offset));
    }
}
