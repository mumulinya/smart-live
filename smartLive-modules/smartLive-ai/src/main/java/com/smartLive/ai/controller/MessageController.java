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
 * AI 消息管理 Controller
 * 处理用户与 AI 的实时对话（基于 SSE 流式响应）以及历史消息查询
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
     * 获取历史消息列表
     * 分页查询指定会话的历史聊天记录
     *
     * @param current 当前页码
     * @param sessionId 会话 ID
     * @return 包含消息实体列表的 Result
     */
    @GetMapping("/list")
    public Result getMessageList(@RequestParam("current") Integer current, @RequestParam("sessionId") Long sessionId) {
        List<Message> list = messageService.selectMessageList(current, sessionId);
        return Result.ok(list);
    }

    /**
     * AI 智能对话接口
     * 采用 Server-Sent Events (SSE) 技术实现流式文本输出，提供流畅的打字机交互体验
     *
     * @param messageDTO 包含用户消息、会话上下文及地理位置等信息的 DTO
     * @return SSE 流，数据事件名为 "message"（正文分片）或 "card_render"（推荐卡片）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(MessageDTO messageDTO) {
        return messageService.chat(messageDTO);
    }
}
