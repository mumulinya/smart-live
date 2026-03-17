package com.smartLive.ai.service.merchant.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.ai.config.memory.ChatMemoryConfiguration;
import com.smartLive.ai.domain.MerchantAiMessage;
import com.smartLive.ai.mapper.MerchantAiMessageMapper;
import com.smartLive.common.redis.service.RedisService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 商家消息聊天记忆管理器。
 */
@Slf4j
@Component
public class MerchantMessageChatMemoryManager {

    private static final String REDIS_KEY_PREFIX = "ai:merchant:chat:memory:list:";
    private static final long REDIS_TTL_DAYS = 7L;

    private final MerchantAiMessageMapper messageMapper;
    private final ChatMemory chatMemory;
    private final RedisService redisService;

    public MerchantMessageChatMemoryManager(MerchantAiMessageMapper messageMapper,
                                            ChatMemory chatMemory,
                                            RedisService redisService) {
        this.messageMapper = messageMapper;
        this.chatMemory = chatMemory;
        this.redisService = redisService;
    }

    /**
     * 重建会话记忆。
     */
    public void rebuildConversationMemory(String conversationId, Long sessionId) {
        if (!hasText(conversationId) || sessionId == null) {
            return;
        }

        chatMemory.clear(conversationId);
        List<Message> historyMessages = loadHistoryMessages(sessionId);
        if (historyMessages.isEmpty()) {
            return;
        }
        chatMemory.add(conversationId, historyMessages);
    }

    /**
     * 追加消息缓存。
     */
    public void appendMessageToCache(MerchantAiMessage message) {
        if (message == null || message.getSessionId() == null) {
            return;
        }

        CachedMessage cachedMessage = toCachedMessage(message);
        if (cachedMessage == null) {
            return;
        }

        String cacheKey = buildRedisKey(message.getSessionId());
        try {
            redisService.rightPushCacheList(cacheKey, cachedMessage);
            redisService.trimCacheList(cacheKey, -ChatMemoryConfiguration.MAX_MESSAGES, -1);
            redisService.expire(cacheKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            if (isWrongTypeException(e)) {
                redisService.deleteObject(cacheKey);
            }
            log.warn("Append merchant chat memory cache failed, key={}", cacheKey, e);
        }
    }

    /**
     * 加载历史消息。
     */
    private List<Message> loadHistoryMessages(Long sessionId) {
        List<CachedMessage> cachedMessages = loadMessagesFromRedis(sessionId);
        if (cachedMessages != null) {
            return toMemoryMessages(cachedMessages);
        }

        List<CachedMessage> dbMessages = loadMessagesFromDb(sessionId);
        refillConversationCache(sessionId, dbMessages);
        return toMemoryMessages(dbMessages);
    }

    /**
     * 从 Redis 加载消息。
     */
    private List<CachedMessage> loadMessagesFromRedis(Long sessionId) {
        String cacheKey = buildRedisKey(sessionId);
        if (!Boolean.TRUE.equals(redisService.hasKey(cacheKey))) {
            return null;
        }
        try {
            List<CachedMessage> cachedMessages = redisService.getCacheListRange(cacheKey, 0, -1);
            if (cachedMessages == null || cachedMessages.isEmpty()) {
                redisService.expire(cacheKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
                return List.of();
            }
            List<CachedMessage> trimmedMessages = trimToWindow(cachedMessages);
            redisService.trimCacheList(cacheKey, -ChatMemoryConfiguration.MAX_MESSAGES, -1);
            redisService.expire(cacheKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
            return trimmedMessages;
        } catch (Exception e) {
            if (isWrongTypeException(e)) {
                redisService.deleteObject(cacheKey);
            }
            log.warn("Read merchant chat memory cache failed, key={}", cacheKey, e);
            return null;
        }
    }

    /**
     * 从数据库加载消息。
     */
    private List<CachedMessage> loadMessagesFromDb(Long sessionId) {
        Page<MerchantAiMessage> page = new Page<>(1, ChatMemoryConfiguration.MAX_MESSAGES, false);
        LambdaQueryWrapper<MerchantAiMessage> queryWrapper = new LambdaQueryWrapper<MerchantAiMessage>()
                .eq(MerchantAiMessage::getSessionId, sessionId)
                .orderByDesc(MerchantAiMessage::getCreateTime)
                .orderByDesc(MerchantAiMessage::getId);

        List<MerchantAiMessage> records = messageMapper.selectPage(page, queryWrapper).getRecords();
        if (records.isEmpty()) {
            return List.of();
        }

        Collections.reverse(records);
        List<CachedMessage> historyMessages = new ArrayList<>(records.size());
        for (MerchantAiMessage record : records) {
            CachedMessage cachedMessage = toCachedMessage(record);
            if (cachedMessage != null) {
                historyMessages.add(cachedMessage);
            }
        }
        return trimToWindow(historyMessages);
    }

    /**
     * 回填会话缓存。
     */
    private void refillConversationCache(Long sessionId, List<CachedMessage> messages) {
        String cacheKey = buildRedisKey(sessionId);
        try {
            redisService.deleteObject(cacheKey);
            if (messages != null && !messages.isEmpty()) {
                redisService.rightPushAllCacheList(cacheKey, messages);
                redisService.trimCacheList(cacheKey, -ChatMemoryConfiguration.MAX_MESSAGES, -1);
                redisService.expire(cacheKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("Refill merchant chat memory cache failed, key={}", cacheKey, e);
        }
    }

    /**
     * 转换为记忆消息。
     */
    private List<Message> toMemoryMessages(List<CachedMessage> cachedMessages) {
        if (cachedMessages == null || cachedMessages.isEmpty()) {
            return List.of();
        }

        List<Message> messages = new ArrayList<>(cachedMessages.size());
        for (CachedMessage cachedMessage : cachedMessages) {
            String role = normalizeRole(cachedMessage.getRole());
            String content = normalizeContent(cachedMessage.getContent());
            if (!hasText(content)) {
                continue;
            }
            switch (role) {
                case "assistant" -> messages.add(new AssistantMessage(content));
                case "system" -> messages.add(new SystemMessage(content));
                default -> messages.add(new UserMessage(content));
            }
        }
        return messages;
    }

    /**
     * 转换为缓存消息。
     */
    private CachedMessage toCachedMessage(MerchantAiMessage message) {
        String content = normalizeContent(message.getContent());
        if (!hasText(content)) {
            return null;
        }
        CachedMessage cachedMessage = new CachedMessage();
        cachedMessage.setMessageId(message.getId());
        cachedMessage.setRole(normalizeRole(message.getRole()));
        cachedMessage.setContent(content);
        return cachedMessage;
    }

    /**
     * 规范化角色。
     */
    private String normalizeRole(String role) {
        if (!hasText(role)) {
            return "user";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化内容。
     */
    private String normalizeContent(String content) {
        if (!hasText(content)) {
            return null;
        }
        return content.trim();
    }

    /**
     * 裁剪窗口数据。
     */
    private List<CachedMessage> trimToWindow(List<CachedMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int size = messages.size();
        int maxSize = ChatMemoryConfiguration.MAX_MESSAGES;
        if (size <= maxSize) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(size - maxSize, size));
    }

    /**
     * 构建 Redis 键。
     */
    private String buildRedisKey(Long sessionId) {
        return REDIS_KEY_PREFIX + sessionId;
    }

    /**
     * 判断是否为 WRONGTYPE 异常。
     */
    private boolean isWrongTypeException(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return false;
        }
        return exception.getMessage().toUpperCase(Locale.ROOT).contains("WRONGTYPE");
    }

    /**
     * 判断文本是否存在。
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 缓存消息类。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CachedMessage {
        private Long messageId;
        private String role;
        private String content;
    }
}
