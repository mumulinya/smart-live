package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.UserAiMessage;
import com.smartLive.ai.service.user.IUserAiMessageService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 用户 AI 聊天控制器。
 */
@Slf4j
@RestController
@RequestMapping("/message")
public class UserAiChatController extends BaseController {

    @Autowired
    private IUserAiMessageService messageService;

    /**
     * 查询消息列表。
     */
    @GetMapping("/list")
    public Result getMessageList(@RequestParam("current") Integer current, @RequestParam("sessionId") Long sessionId) {
        List<UserAiMessage> list = messageService.selectMessageList(current, sessionId);
        return Result.ok(list);
    }

    /**
     * 处理聊天服务端事件流。
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(MessageDTO messageDTO) {
        return messageService.chat(messageDTO);
    }
}
