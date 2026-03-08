package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Message Controller
 *
 * @author smartLive
 */
@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController extends BaseController {

    @Autowired
    private IMessageService messageService;

    /**
     * Get message list (history)
     */
    @GetMapping("/list")
    public Result getMessageList(@RequestParam("current") Integer current, @RequestParam("sessionId") Long sessionId) {
        List<Message> list = messageService.selectMessageList(current, sessionId);
        return Result.ok(list);
    }

    /**
     * AI Chat (SSE)
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(MessageDTO messageDTO) {
        return messageService.chat(messageDTO);
    }
}
