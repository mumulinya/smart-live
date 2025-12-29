package com.smartLive.ai.controller;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.repository.ChatHistoryRepository;
import com.smartLive.ai.service.strategy.handlers.impl.CommentHandler;
import com.smartLive.ai.service.orchestration.AIChatOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/ai")
@Validated
public class AIChatController {
    
    @Autowired
    private AIChatOrchestrator aiOrchestrator;

    @Autowired
    private CommentHandler commentHandler;
    @Autowired
    private  ChatHistoryRepository chatHistoryRepository;
    
    @PostMapping("/chat")
    public Flux<String> chat(@RequestBody AIChatRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("📨 收到AI聊天请求: sessionId={}, message={}", 
                request.getSessionId(), request.getMessage());
        
        try {
            // 1.保存会话id
            chatHistoryRepository.save("chat", request.getSessionId());
            Flux<String> response = aiOrchestrator.processMessage(request);
            return response;
            
        } catch (Exception e) {
            log.error("❌ AI聊天处理失败: {}", request.getMessage(), e);
            return Flux.just("对不起，我无法理解你的问题。");
        }
    }
    @PostMapping("/crateComment")
    public void crateComment(@RequestBody Map<Integer, Set<Long>> map) {
//        commentHandler.aiCreateComment(map);
    }
}