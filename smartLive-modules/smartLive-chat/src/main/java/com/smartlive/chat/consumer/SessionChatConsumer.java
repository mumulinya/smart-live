package com.smartlive.chat.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.dto.ChatMessageEvent;
import com.smartlive.chat.handle.NettyChatHandler; // ✅ 引入 Netty 处理器
import com.smartlive.chat.service.IChatMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
/**
 * 会话消息消费者
 */
@Slf4j
@Component
public class SessionChatConsumer {
    @Autowired
    private IChatMessagesService chatMessagesService;

    @Autowired
    private ObjectMapper objectMapper; // ✅ 引入 Jackson 工具，用于生成 JSON

    /**
     * 处理聊天消息 - 推送给双方用户
     */
    public void processChatMessage(ChatMessageEvent messageEvent) {
        Long fromUserId = messageEvent.getFromUserId();
        Long toUserId = messageEvent.getToUserId();
        Long sessionId = messageEvent.getSessionId();
        Long messageId = messageEvent.getMessageId();

        log.info("📢 处理会话消息: 发送方={}, 接收方={}, sessionId={}", fromUserId, toUserId, sessionId);

        try {
            // 🔥 关键修改 1：使用 NettyChatHandler 的静态方法判断状态
            boolean isReceiverInChatSession = NettyChatHandler.isUserInChatSession(toUserId, sessionId);
            Long messageStatus = isReceiverInChatSession ? 1L : 2L; // 1已读，2已送达

            // 构建消息数据
            Map<String, Object> messageData = Map.of(
                    "id", messageId,
                    "sessionId", sessionId,
                    "fromUid", fromUserId,
                    "toUid", toUserId,
                    "content", messageEvent.getContent(),
                    "status", messageStatus,
                    "createTime", messageEvent.getCreateTime()
            );

            // 🔥 关键修改 2：推送给发送方 (替换旧代码)
            if (NettyChatHandler.isUserOnline(fromUserId)) {
                sendNettyMessage(fromUserId, "NEW_MESSAGE", messageData);
                log.info("✅ NEW_MESSAGE 已推送给发送方: {}", fromUserId);
            } else {
                log.info("发送方 {} 离线", fromUserId);
            }

            // 🔥 关键修改 3：推送给接收方 (替换旧代码)
            if (NettyChatHandler.isUserOnline(toUserId)) {
                sendNettyMessage(toUserId, "NEW_MESSAGE", messageData);
                log.info("✅ NEW_MESSAGE 已推送给接收方: {}", toUserId);

                // 更新消息状态
                ChatMessages message = new ChatMessages();
                message.setId(messageId);
                message.setStatus(messageStatus);
                chatMessagesService.updateById(message);

                log.info("📝 消息状态更新为: {}", isReceiverInChatSession ? "已读" : "已送达");

                // 如果是已读，通知发送方
                if (isReceiverInChatSession) {
                    notifySenderMessageRead(fromUserId, messageId, sessionId);
                }
            } else {
                log.info("接收方 {} 离线，消息保留在会话队列中", toUserId);
                // 离线用户默认设置为已送达状态
                ChatMessages message = new ChatMessages();
                message.setId(messageId);
                message.setStatus(2L);
                chatMessagesService.updateById(message);
            }

            log.info("🎉 会话消息处理完成: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("处理会话消息异常", e);
        }
    }

    /**
     * 🔥 新增辅助方法：通过 Netty 发送格式化的 JSON 消息
     */
    private void sendNettyMessage(Long userId, String type, Object data) {
        try {
            // 构造符合前端协议的 JSON: { "type": "...", "data": ... }
            Map<String, Object> payload = Map.of(
                    "type", type,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            );
            String jsonString = objectMapper.writeValueAsString(payload);

            // 调用 Netty 的静态推送方法
            NettyChatHandler.pushMessageToUser(userId, jsonString);
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
        }
    }

    /**
     * 通知发送方消息已读
     */
    private void notifySenderMessageRead(Long fromUserId, Long messageId, Long sessionId) {
        if (NettyChatHandler.isUserOnline(fromUserId)) {
            Map<String, Object> readNotification = Map.of(
                    "type", "MESSAGE_READ",
                    "messageId", messageId,
                    "sessionId", sessionId,
                    "status", 1L
            );
            // 调用上面的辅助方法
            sendNettyMessage(fromUserId, "MESSAGE_STATUS_UPDATE", readNotification);
            log.info("✅ 已通知发送方 {} 消息 {} 已读", fromUserId, messageId);
        }
    }
}