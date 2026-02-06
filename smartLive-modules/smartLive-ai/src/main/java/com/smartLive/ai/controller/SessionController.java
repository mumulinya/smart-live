package com.smartLive.ai.controller;

import com.smartLive.ai.domain.Session;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Session Controller
 *
 * @author smartLive
 */
@Slf4j
@RestController
@RequestMapping("/session")
public class SessionController extends BaseController {

    @Autowired
    private ISessionService sessionService;

    /**
     * Create new session
     */
    @PostMapping("/create")
    public Result createSession(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        Long sessionId = sessionService.createSession(title);
        return Result.ok(sessionId);
    }

    /**
     * Get session list
     */
    @GetMapping("/list")
    public Result getSessionList(@RequestParam("current") Integer current) {
        List<Session> list = sessionService.selectSessionList(current);
        return Result.ok(list);
    }
    /**
     * 搜索用户会话
     */
    @GetMapping("/search")
    public Result searchSession(
            @RequestParam("keyword") String keyword,
            @RequestParam("current") Integer current) {
        List<Session> list = sessionService.searchByKeyword(keyword, current);
        return Result.ok(list);
    }
}
