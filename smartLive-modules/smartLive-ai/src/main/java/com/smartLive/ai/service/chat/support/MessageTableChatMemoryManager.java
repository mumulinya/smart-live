package com.smartLive.ai.service.chat.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.ai.config.memory.ChatMemoryConfiguration;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.mapper.MessageMapper;
import com.smartLive.common.redis.service.RedisService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * ChatMemory 会话装载器：
 * 1. 请求开始时先清空运行时 ChatMemory，再按会话重建；
 * 2. 历史优先从 Redis List 读取，未命中时回源 message 表；
 * 3. 每轮 user/assistant 落库后把同一份摘要消息追加到 Redis List。
 */
public class MessageTableChatMemoryManager {

    private static final String REDIS_KEY_PREFIX = "ai:chat:memory:list:";
    private static final long REDIS_TTL_DAYS = 7L;

    private final MessageMapper messageMapper;
    private final RecommendationCardHelper recommendationCardHelper;
    private final ChatMemory chatMemory;
    private final RedisService redisService;

    public void rebuildConversationMemory(String conversationId, Long sessionId, boolean contextEnabled) {
        if (!hasText(conversationId)) {
            return;
        }

        // 先清空旧窗口，保证本次请求只使用最新重建后的上下文。
        chatMemory.clear(conversationId);
        if (!contextEnabled || sessionId == null) {
            return;
        }

        List<org.springframework.ai.chat.messages.Message> historyMessages = loadHistoryMessages(sessionId);
        if (historyMessages.isEmpty()) {
            return;
        }

        chatMemory.add(conversationId, historyMessages);
        log.debug("Hydrated {} history messages into chat memory, conversationId={}", historyMessages.size(), conversationId);
    }

    /**
     * 当前轮落库后，把消息摘要追加到 Redis List，Redis 自动维护窗口。
     */
    public void appendMessageToCache(Message message) {
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
        }
        catch (Exception e) {
            // key 类型异常时先清理，避免污染后续会话。
            if (isWrongTypeException(e)) {
                redisService.deleteObject(cacheKey);
            }
            log.warn("Failed to append chat memory cache, key={}", cacheKey, e);
        }
    }

    private List<org.springframework.ai.chat.messages.Message> loadHistoryMessages(Long sessionId) {
        // 优先走 Redis List，命中则不查库；miss 时回源 DB 并回填 Redis。
        List<CachedMessage> cachedMessages = loadMessagesFromRedis(sessionId);
        if (cachedMessages != null) {
            return toMemoryMessages(cachedMessages);
        }

        List<CachedMessage> dbMessages = loadMessagesFromDb(sessionId);
        refillConversationCache(sessionId, dbMessages);
        return toMemoryMessages(dbMessages);
    }

    /**
     * 返回 null 表示缓存未命中，需要回源 DB。
     * 返回空列表表示缓存命中但无历史。
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
            // 读取时也做一次窗口纠偏，确保 Redis 和 ChatMemory 窗口一致。
            redisService.trimCacheList(cacheKey, -ChatMemoryConfiguration.MAX_MESSAGES, -1);
            redisService.expire(cacheKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
            return trimmedMessages;
        }
        catch (Exception e) {
            // key 类型异常说明缓存脏了，删掉后走 DB 回源重建。
            if (isWrongTypeException(e)) {
                redisService.deleteObject(cacheKey);
            }
            log.warn("Failed to read chat memory cache, key={}", cacheKey, e);
            return null;
        }
    }

    private List<CachedMessage> loadMessagesFromDb(Long sessionId) {
        // 按窗口上限读取最近历史，查出后再反转成模型需要的时间正序。
        Page<Message> page = new Page<>(1, ChatMemoryConfiguration.MAX_MESSAGES, false);
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, sessionId)
                .orderByDesc(Message::getCreateTime)
                .orderByDesc(Message::getId);

        List<Message> records = messageMapper.selectPage(page, queryWrapper).getRecords();
        if (records.isEmpty()) {
            return List.of();
        }

        Collections.reverse(records);
        List<CachedMessage> historyMessages = new ArrayList<>(records.size());
        for (Message record : records) {
            CachedMessage memoryMessage = toCachedMessage(record);
            if (memoryMessage != null) {
                historyMessages.add(memoryMessage);
            }
        }
        return trimToWindow(historyMessages);
    }

    private void refillConversationCache(Long sessionId, List<CachedMessage> messages) {
        String cacheKey = buildRedisKey(sessionId);
        try {
            // miss 回填时直接重建该 key，避免残留脏数据。
            redisService.deleteObject(cacheKey);
            if (messages != null && !messages.isEmpty()) {
                redisService.rightPushAllCacheList(cacheKey, messages);
                redisService.trimCacheList(cacheKey, -ChatMemoryConfiguration.MAX_MESSAGES, -1);
                redisService.expire(cacheKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
            }
        }
        catch (Exception e) {
            log.warn("Failed to refill chat memory cache from DB, key={}", cacheKey, e);
        }
    }

    private List<org.springframework.ai.chat.messages.Message> toMemoryMessages(List<CachedMessage> cachedMessages) {
        if (cachedMessages == null || cachedMessages.isEmpty()) {
            return List.of();
        }

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>(cachedMessages.size());
        for (CachedMessage cachedMessage : cachedMessages) {
            String role = normalizeRole(cachedMessage.getRole());
            String content = normalizeContent(role, cachedMessage.getContent());
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

    private CachedMessage toCachedMessage(Message record) {
        String role = normalizeRole(record.getRole());
        String content = normalizeContent(role, record.getContent());
        if (!hasText(content)) {
            return null;
        }

        CachedMessage cachedMessage = new CachedMessage();
        cachedMessage.setMessageId(record.getId());
        cachedMessage.setRole(role);
        cachedMessage.setContent(content);
        return cachedMessage;
    }

    private String normalizeRole(String role) {
        if (!hasText(role)) {
            return "user";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeContent(String role, String content) {
        if (!hasText(content)) {
            return null;
        }

        if ("assistant".equals(role)) {
            // 助手若返回推荐卡片，缓存中只保留“replyText + 核心实体摘要”。
            String summary = recommendationCardHelper.buildMemorySummary(content);
            if (hasText(summary)) {
                return summary;
            }
            if (recommendationCardHelper.extractRecommendationJson(content) != null) {
                // 卡片摘要构建失败时，至少保留 replyText，避免把整段 JSON 放进记忆。
                return recommendationCardHelper.extractReplyText(content);
            }
        }

        return content.trim();
    }

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

    private boolean isWrongTypeException(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return false;
        }
        return exception.getMessage().toUpperCase(Locale.ROOT).contains("WRONGTYPE");
    }

    private String buildRedisKey(Long sessionId) {
        return REDIS_KEY_PREFIX + sessionId;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CachedMessage {
        /**
         * 数据库 message.id，便于排查问题时对照。
         */
        private Long messageId;
        private String role;
        private String content;
    }
}