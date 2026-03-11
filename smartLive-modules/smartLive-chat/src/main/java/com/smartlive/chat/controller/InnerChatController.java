package com.smartlive.chat.controller;

import com.smartLive.common.core.domain.R;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.domain.UserSessions;
import com.smartlive.chat.service.IChatMessagesService;
import com.smartlive.chat.service.IUserSessionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天内部服务控制器
 * 提供给 IM 服务调用的 RPC 接口，负责跨服务的消息持久化与会话同步。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/inner/chat")
public class InnerChatController {

    @Autowired
    private IChatMessagesService chatMessagesService;

    @Autowired
    private IUserSessionsService userSessionsService;

    @PostMapping("/msg")
    public Long saveMessage(@RequestBody ChatMessages chatMessage) {
        chatMessagesService.save(chatMessage);
        return chatMessage.getId();
    }

    @PostMapping("/session")
    public Boolean syncSession(@RequestBody UserSessions userSession) {
        userSessionsService.isCreateUserSessions(userSession);
        return true;
    }
}
