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
 * 用户聊天消息Controller
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@RestController
@RequestMapping("/message")
public class ChatMessagesController extends BaseController
{
    @Autowired
    private IChatMessagesService chatMessagesService;

    /**
     * 查询用户聊天消息列表
     */
    @GetMapping("/list")
    public Result list(ChatMessages chatMessages,@RequestParam("current") Integer current)
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
     * 新增用户聊天消息
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
}
