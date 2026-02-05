package com.smartLive.ai.controller;

import com.smartLive.ai.domain.Session;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
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
    public AjaxResult createSession(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        Long sessionId = sessionService.createSession(title);
        return AjaxResult.success("Session created successfully", sessionId);
    }

    /**
     * Get session list
     */
    @GetMapping("/list")
    public TableDataInfo getSessionList(Session session) {
        startPage();
        List<Session> list = sessionService.selectSessionList(session);
        return getDataTable(list);
    }
}
