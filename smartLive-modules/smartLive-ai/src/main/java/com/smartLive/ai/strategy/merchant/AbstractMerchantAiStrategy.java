package com.smartLive.ai.strategy.merchant;

import com.smartLive.ai.domain.AiMerchantMessage;
import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Date;

@Slf4j
public abstract class AbstractMerchantAiStrategy {

    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient merchantStrategyChatClient;
    protected final IAiMerchantSessionService merchantSessionService;
    protected final IAiMerchantMessageService merchantMessageService;
    protected final MerchantMessageChatMemoryManager memoryManager;

    protected AbstractMerchantAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                                         IAiMerchantSessionService merchantSessionService,
                                         IAiMerchantMessageService merchantMessageService,
                                         MerchantMessageChatMemoryManager memoryManager) {
        this.merchantStrategyChatClient = merchantStrategyChatClient;
        this.merchantSessionService = merchantSessionService;
        this.merchantMessageService = merchantMessageService;
        this.memoryManager = memoryManager;
    }

    public final Flux<String> execute(Long userId, MerchantChatDTO dto) {
        validateInput(userId, dto);
        AiMerchantSession session = merchantSessionService.getAndCheckSession(userId, dto.getSessionId());
        validateSession(dto, session);

        String conversationId = buildConversationId(dto.getSessionId());
        memoryManager.rebuildConversationMemory(conversationId, dto.getSessionId());

        AiMerchantMessage userRecord = merchantMessageService.saveMessage(dto.getSessionId(), "user", dto.getMessage().trim());
        memoryManager.appendMessageToCache(userRecord);
        touchSession(dto.getSessionId());

        String systemPrompt = buildSystemPrompt(dto, session);
        String userPrompt = buildUserMessage(dto, session);
        StringBuilder aiReply = new StringBuilder();

        return merchantStrategyChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .stream()
                .content()
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        aiReply.append(chunk);
                    }
                })
                .doFinally(signalType -> saveAssistantMessage(dto.getSessionId(), aiReply.toString()))
                .doOnError(ex -> log.error("Merchant AI chat failed, sessionId={}", dto.getSessionId(), ex));
    }

    protected abstract String getRolePrompt();

    protected String buildSystemPrompt(MerchantChatDTO dto, AiMerchantSession session) {
        return "You are a merchant AI assistant for SmartLive.\n"
                + "Please answer in Chinese.\n"
                + getRolePrompt()
                + "\nKeep reply concise, practical and actionable within 300 Chinese characters.";
    }

    protected String buildUserMessage(MerchantChatDTO dto, AiMerchantSession session) {
        return dto.getMessage();
    }

    private void saveAssistantMessage(Long sessionId, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        AiMerchantMessage assistantRecord = merchantMessageService.saveMessage(sessionId, "assistant", content);
        memoryManager.appendMessageToCache(assistantRecord);
        touchSession(sessionId);
    }

    private void touchSession(Long sessionId) {
        AiMerchantSession session = new AiMerchantSession();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        merchantSessionService.updateById(session);
    }

    private void validateInput(Long userId, MerchantChatDTO dto) {
        if (userId == null) {
            throw new ServiceException("User not logged in");
        }
        if (dto == null || dto.getSessionId() == null || dto.getShopId() == null
                || !StringUtils.hasText(dto.getType()) || !StringUtils.hasText(dto.getMessage())) {
            throw new ServiceException("Invalid request");
        }
    }

    private void validateSession(MerchantChatDTO dto, AiMerchantSession session) {
        if (!dto.getShopId().equals(session.getShopId())) {
            throw new ServiceException("Session and shop mismatch");
        }
    }

    private String buildConversationId(Long sessionId) {
        return "merchant::session::" + sessionId;
    }
}
