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
 * AI 对话会话管理 Controller
 * 负责会话的创建、列表查询、重命名及删除等生命周期管理
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
     * 创建新会话
     *
     * @param params 包含会话标题的 Map
     * @return 新创建的会话 ID
     */
    @PostMapping("/create")
    public Result createSession(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        Long sessionId = sessionService.createSession(title);
        return Result.ok(sessionId);
    }

    /**
     * 获取用户的会话列表
     * 按最后活跃时间倒序排列，支持分页
     *
     * @param current 当前页码
     * @return 会话对象列表
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

    /**
     * 删除指定会话
     * 同步删除该会话下的所有聊天消息记录
     *
     * @param sessionId 会话 ID
     * @return 操作成功或失败的 Result
     */
    @DeleteMapping("/{sessionId}")
    public Result deleteSession(@PathVariable("sessionId") Long sessionId) {
        boolean success = sessionService.deleteSession(sessionId);
        if (success) {
            return Result.ok();
        }
        return Result.fail("删除会话失败");
    }

    /**
     * 更新会话标题（重命名）
     *
     * @param sessionId 会话 ID
     * @param params 包含新 title 的 Map
     * @return 操作结果
     */
    @PutMapping("/{sessionId}/title")
    public Result updateSessionTitle(@PathVariable("sessionId") Long sessionId, @RequestBody Map<String, String> params) {
        String title = params.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.fail("标题不能为空");
        }
        boolean success = sessionService.updateSessionTitle(sessionId, title);
        if (success) {
            return Result.ok();
        }
        return Result.fail("更新标题失败");
    }
}
