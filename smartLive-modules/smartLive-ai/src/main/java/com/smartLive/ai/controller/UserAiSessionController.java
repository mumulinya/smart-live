package com.smartLive.ai.controller;

import com.smartLive.ai.domain.UserAiSession;
import com.smartLive.ai.service.IUserAiSessionService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/session")
public class UserAiSessionController extends BaseController {

    @Autowired
    private IUserAiSessionService sessionService;

    @PostMapping("/create")
    public Result createSession(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        Long sessionId = sessionService.createSession(title);
        return Result.ok(sessionId);
    }

    @GetMapping("/list")
    public Result getSessionList(@RequestParam("current") Integer current) {
        List<UserAiSession> list = sessionService.selectSessionList(current);
        return Result.ok(list);
    }

    @GetMapping("/search")
    public Result searchSession(@RequestParam("keyword") String keyword,
                                @RequestParam("current") Integer current) {
        List<UserAiSession> list = sessionService.searchByKeyword(keyword, current);
        return Result.ok(list);
    }

    @DeleteMapping("/{sessionId}")
    public Result deleteSession(@PathVariable("sessionId") Long sessionId) {
        boolean success = sessionService.deleteSession(sessionId);
        if (success) {
            return Result.ok();
        }
        return Result.fail("\u5220\u9664\u4f1a\u8bdd\u5931\u8d25");
    }

    @PutMapping("/{sessionId}/title")
    public Result updateSessionTitle(@PathVariable("sessionId") Long sessionId,
                                     @RequestBody Map<String, String> params) {
        String title = params.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.fail("\u4f1a\u8bdd\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a");
        }
        boolean success = sessionService.updateSessionTitle(sessionId, title);
        if (success) {
            return Result.ok();
        }
        return Result.fail("\u66f4\u65b0\u4f1a\u8bdd\u6807\u9898\u5931\u8d25");
    }
}