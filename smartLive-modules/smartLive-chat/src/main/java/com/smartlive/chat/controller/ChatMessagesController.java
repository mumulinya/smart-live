package com.smartlive.chat.controller;

import java.util.List;

import com.smartLive.common.core.web.domain.Result;
import com.smartlive.chat.service.IChatMessagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartlive.chat.domain.ChatMessages;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;

/**
 * 用户私聊消息控制器
 * 处理私聊消息的发送、历史记录分页查询以及聊天历史日期范围的获取。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/message")
public class ChatMessagesController extends BaseController
{
    @Autowired
    private IChatMessagesService chatMessagesService;

    /**
     * 分页查询指定会话的聊天消息列表
     * 
     * @param chatMessages 查询条件（需包含 sessionId）
     * @param current 当前页码
     * @return 消息列表 R 对象
     */
    @GetMapping("/list")
    public Result list(ChatMessages chatMessages,@RequestParam(value = "current",defaultValue = "1") Integer current)
    {
        List<ChatMessages> list = chatMessagesService.selectChatMessagesList(chatMessages,current);
        return Result.ok(list);
    }

    /**
     * 获取用户聊天消息详细信息
     */
    @RequiresPermissions("chat:chat:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(chatMessagesService.selectChatMessagesById(id));
    }

    /**
     * 发送/推送一条私聊消息
     * 逻辑包含异步推送到 MQ 以及持久化到数据库。
     * 
     * @param chatMessages 消息实体
     * @return 发送结果 AjaxResult
     */
    @Log(title = "用户聊天消息", businessType = BusinessType.INSERT)
    @PostMapping("/send")
    public AjaxResult add(@RequestBody ChatMessages chatMessages)
    {
        return toAjax(chatMessagesService.insertChatMessages(chatMessages));
    }

    /**
     * 修改用户聊天消息
     */
    @RequiresPermissions("chat:chat:edit")
    @Log(title = "用户聊天消息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ChatMessages chatMessages)
    {
        return toAjax(chatMessagesService.updateChatMessages(chatMessages));
    }

    /**
     * 删除用户聊天消息
     */
    @RequiresPermissions("chat:chat:remove")
    @Log(title = "用户聊天消息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(chatMessagesService.deleteChatMessagesByIds(ids));
    }
    /**
     * 获取用户聊天消息历史记录的日期
     */
    @GetMapping("/history/dates")
    public Result getHistoryDates(@RequestParam("sessionId") Long sessionId) {
        return Result.ok(chatMessagesService.getHistoryDates(sessionId));
    }
}
