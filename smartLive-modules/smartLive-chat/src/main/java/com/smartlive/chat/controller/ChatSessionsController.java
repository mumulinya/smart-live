package com.smartlive.chat.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartlive.chat.domain.ChatSessions;
import com.smartlive.chat.service.IChatSessionsService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 私聊会话Controller
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@RestController
@RequestMapping("/chatSession")
public class ChatSessionsController extends BaseController
{
    @Autowired
    private IChatSessionsService chatSessionsService;

    /**
     * 查询私聊会话列表
     */
    @RequiresPermissions("chat:chat:list")
    @GetMapping("/list")
    public TableDataInfo list(ChatSessions chatSessions)
    {
        startPage();
        List<ChatSessions> list = chatSessionsService.selectChatSessionsList(chatSessions);
        return getDataTable(list);
    }

    /**
     * 导出私聊会话列表
     */
    @RequiresPermissions("chat:chat:export")
    @Log(title = "私聊会话", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ChatSessions chatSessions)
    {
        List<ChatSessions> list = chatSessionsService.selectChatSessionsList(chatSessions);
        ExcelUtil<ChatSessions> util = new ExcelUtil<ChatSessions>(ChatSessions.class);
        util.exportExcel(response, list, "私聊会话数据");
    }

    /**
     * 获取私聊会话详细信息
     */
    @GetMapping
    public Result getInfo(@RequestParam("sessionId") Long sessionId)
    {
        ChatSessions chatSessions = chatSessionsService.selectChatSessionsById(sessionId);
        return Result.ok(chatSessions);
    }

    /**
     * 新增私聊会话
     */
    @PostMapping("/createSession/{targetUid}")
    public Result add(@PathVariable("targetUid") Long targetUid)
    {
        Long userId = UserContextHolder.getUser().getId();
        ChatSessions chatSessions = new ChatSessions();
        if(userId>targetUid){
            chatSessions.setMaxUserId(userId);
            chatSessions.setLowUserId(targetUid);
        }else{
            chatSessions.setMaxUserId(targetUid);
        }
        Long sessionsId=chatSessionsService.insertChatSessions(chatSessions);
        return Result.ok(sessionsId);
    }

    /**
     * 修改私聊会话
     */
    @RequiresPermissions("chat:chat:edit")
    @Log(title = "私聊会话", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ChatSessions chatSessions)
    {
        return toAjax(chatSessionsService.updateChatSessions(chatSessions));
    }

    /**
     * 删除私聊会话
     */
    @RequiresPermissions("chat:chat:remove")
    @Log(title = "私聊会话", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(chatSessionsService.deleteChatSessionsByIds(ids));
    }

    @GetMapping("/getSessionId")
    public Result getSessionId(ChatSessions chatSessions) {
        if(chatSessions.getFromUid()>chatSessions.getToUid()){
            chatSessions.setMaxUserId(chatSessions.getFromUid());
            chatSessions.setLowUserId(chatSessions.getToUid());
        }else{
            chatSessions.setMaxUserId(chatSessions.getToUid());
            chatSessions.setLowUserId(chatSessions.getFromUid());
        }
        return Result.ok(chatSessionsService.getSessionId(chatSessions));
    }
}
