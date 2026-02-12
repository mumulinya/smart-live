package com.smartlive.chat.controller;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import com.smartlive.chat.service.ISystemNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/app/notice/system", "/notice/system"})
public class SystemNoticeController {

    @Autowired
    private ISystemNoticeService systemNoticeService;

    @GetMapping("/list")
    public Result list(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(systemNoticeService.queryNoticePage(userId, current));
    }

    @PutMapping("/read/{noticeId}")
    public Result read(@PathVariable("noticeId") String noticeId) {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(systemNoticeService.markRead(userId, noticeId));
    }

    @PutMapping("/read/all")
    public Result readAll() {
        Long userId = UserContextHolder.getUser().getId();
        int affected = systemNoticeService.markAllRead(userId);
        return Result.ok(Map.of("affected", affected));
    }

    @GetMapping("/unread/count")
    public Result unreadCount() {
        Long userId = UserContextHolder.getUser().getId();
        return Result.ok(Map.of("count", systemNoticeService.countUnread(userId)));
    }
}
