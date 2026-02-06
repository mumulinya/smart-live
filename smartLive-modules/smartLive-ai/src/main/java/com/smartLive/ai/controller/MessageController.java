package com.smartLive.ai.controller;

import com.smartLive.ai.domain.DTO.MessageDTO;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.ai.service.orchestration.AIChatOrchestrator;
import com.smartLive.ai.service.rag.impl.ShopRagService;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

    @Autowired
    private AIChatOrchestrator aiChatOrchestrator;

    @Autowired
    private ShopRagService shopRagService;

    /**
     * Get message list (history)
     */
    @GetMapping("/list")
    public Result getMessageList(@RequestParam("current") Integer current, @RequestParam("sessionId") Long sessionId) {
        List<Message> list = messageService.selectMessageList(current,sessionId);
        return Result.ok(list);
    }

    /**
     * AI Chat (SSE)
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(MessageDTO messageDTO) {

        // 1. Save User Message
        Long sessionId = messageDTO.getSessionId();
        Long userId = messageDTO.getUserId();
        String message = messageDTO.getMessage();
        messageService.saveMessage(sessionId, "user", message);

        // 2. Build Request
        AIChatRequest request = new AIChatRequest();
        request.setMessage(message);
        request.setSessionId(String.valueOf(sessionId));
        request.setUserId(String.valueOf(userId));
        request.setX(messageDTO.getX());
        request.setY(messageDTO.getY());
        // (Optional) Context logic can be added here
        if (messageDTO.getContextMode()) {
             // Retrieve context if needed
        }
        // 3. Call Orchestrator & Save Assistant Response
        StringBuilder fullResponse = new StringBuilder();

        return aiChatOrchestrator.processMessage(request)
                .doOnNext(chunk -> fullResponse.append(chunk)) // Accumulate chunks
                .doFinally(signalType -> {
                    // Save complete assistant message when stream ends
                    String responseText = fullResponse.toString();
                    if (!responseText.isEmpty()) {
                        try {
                            messageService.saveMessage(sessionId, "assistant", responseText);
                            log.info("✅ Saved assistant response for session {}", sessionId);
                        } catch (Exception e) {
                            log.error("❌ Failed to save assistant response", e);
                        }
                    }
                });
    }
}
