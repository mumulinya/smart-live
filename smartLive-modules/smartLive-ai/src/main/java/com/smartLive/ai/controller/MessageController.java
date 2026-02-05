package com.smartLive.ai.controller;

import com.smartLive.ai.domain.Message;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.ai.service.orchestration.AIChatOrchestrator;
import com.smartLive.common.core.web.controller.BaseController;
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

    /**
     * Get message list (history)
     */
    @GetMapping("/list")
    public TableDataInfo getMessageList(@RequestParam Long sessionId) {
        startPage();
        List<Message> list = messageService.selectMessageList(sessionId);
        return getDataTable(list);
    }

    /**
     * AI Chat (SSE)
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam Long sessionId,
            @RequestParam String message,
            @RequestParam(defaultValue = "false") Boolean contextMode) {

        Long userId = SecurityUtils.getUserId();

        // 1. Save User Message
        messageService.saveMessage(sessionId, "user", message);

        // 2. Build Request
        AIChatRequest request = new AIChatRequest();
        request.setMessage(message);
        request.setSessionId(String.valueOf(sessionId));
        request.setUserId(String.valueOf(userId));

        // (Optional) Context logic can be added here
        if (contextMode) {
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
