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
 * 内部调用控制器 - 供 smart-live-im 调用
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
