package com.smartlive.chat.consumer;
import com.smartLive.common.core.constant.mq.ChatMqConstants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.redis.service.RedisService;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.dto.ChatMessageEvent;
import com.smartlive.chat.service.IChatMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 会话消息消费者 (Refactored for decoupled architecture)
 */
@Slf4j
@Component
public class SessionChatConsumer {
    @Autowired
    private IChatMessagesService chatMessagesService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 处理聊天消息 - 推送给双方用户
     */
    public void processChatMessage(ChatMessageEvent messageEvent) {
        Long fromUserId = messageEvent.getFromUserId();
        Long toUserId = messageEvent.getToUserId();
        Long sessionId = messageEvent.getSessionId();
        Long messageId = messageEvent.getMessageId(); // Note: might be 0 if not persisted in caller, but IM persists it via Feign now.

        log.info("📢 处理会话消息: 发送方={}, 接收方={}, sessionId={}", fromUserId, toUserId, sessionId);

        try {
            // Check Redis for receiver's active session
            Long receiverActiveSession = redisService.getCacheObject(RedisConstants.IM_SESSION_KEY + toUserId);
            boolean isReceiverInChatSession = (receiverActiveSession != null && receiverActiveSession.equals(sessionId));

            Long messageStatus = isReceiverInChatSession ? 1L : 2L; // 1已读，2已送达

            // 构建消息数据
            Map<String, Object> messageData = Map.of(
                    "id", messageId != null ? messageId : 0L,
                    "sessionId", sessionId,
                    "fromUid", fromUserId,
                    "toUid", toUserId,
                    "content", messageEvent.getContent(),
                    "status", messageStatus,
                    "createTime", messageEvent.getCreateTime()
            );

            // 推送给发送方
            if (isUserOnline(fromUserId)) {
                sendPushToIm(fromUserId, "NEW_MESSAGE", messageData);
                log.info("✅ NEW_MESSAGE 已推送给发送方: {}", fromUserId);
            }

            // 推送给接收方
            if (isUserOnline(toUserId)) {
                sendPushToIm(toUserId, "NEW_MESSAGE", messageData);
                log.info("✅ NEW_MESSAGE 已推送给接收方: {}", toUserId);

                // 更新消息状态
                // Note: We need a valid messageId here. If 0, update might fail.
                // Assuming Feign save worked, but we might not have the ID if we didn't return it.
                // For safety, in a real scenario, ChatService should return the ID.
                if (messageId != null && messageId > 0) {
                    ChatMessages message = new ChatMessages();
                    message.setId(messageId);
                    message.setStatus(messageStatus);
                    chatMessagesService.updateById(message);
                    log.info("📝 消息状态更新为: {}", isReceiverInChatSession ? "已读" : "已送达");
                }

                // 如果是已读，通知发送方
                if (isReceiverInChatSession) {
                    notifySenderMessageRead(fromUserId, messageId, sessionId);
                }
            } else {
                log.info("接收方 {} 离线，消息保留在会话队列中", toUserId);
                if (messageId != null && messageId > 0) {
                    ChatMessages message = new ChatMessages();
                    message.setId(messageId);
                    message.setStatus(2L);
                    chatMessagesService.updateById(message);
                }
            }

            log.info("🎉 会话消息处理完成: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("处理会话消息异常", e);
        }
    }

    private boolean isUserOnline(Long userId) {
        return redisService.hasKey(RedisConstants.IM_ONLINE_KEY + userId);
    }

    /**
     * 发送推送指令到 RabbitMQ
     */
    private void sendPushToIm(Long userId, String type, Object data) {
        try {
            // 1. 构造内层数据 JSON
            Map<String, Object> payload = Map.of(
                    "type", type,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            );
            String innerJson = objectMapper.writeValueAsString(payload);

            // 2. 构造外层 MQ 消息
            Map<String, Object> mqMap = Map.of(
                "userId", userId,
                "json", innerJson
            );

            // 3. 直接发送 Map 对象，由 MessageConverter 处理序列化
            // 不要手动序列化 mqMap，否则会导致双重序列化，接收端解析失败
            rabbitTemplate.convertAndSend(ChatMqConstants.CHAT_EXCHANGE_NAME, "im.push.user", mqMap);

        } catch (Exception e) {
            log.error("发送MQ推送失败", e);
        }
    }

    /**
     * 通知发送方消息已读
     */
    private void notifySenderMessageRead(Long fromUserId, Long messageId, Long sessionId) {
        if (isUserOnline(fromUserId)) {
            Map<String, Object> readNotification = Map.of(
                    "type", "MESSAGE_READ",
                    "messageId", messageId != null ? messageId : 0L,
                    "sessionId", sessionId,
                    "status", 1L
            );
            sendPushToIm(fromUserId, "MESSAGE_STATUS_UPDATE", readNotification);
            log.info("✅ 已通知发送方 {} 消息 {} 已读", fromUserId, messageId);
        }
    }
}
