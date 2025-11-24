package com.smartlive.chat.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.utils.rabbitMq.MqDeadLetterSendUtils;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.dto.ChatMessageEvent;
import com.smartlive.chat.handle.ChatWebSocketHandler;
import com.smartlive.chat.service.IChatMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class SessionChatConsumer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private IChatMessagesService chatMessagesService;
    /**
     * 处理聊天消息 - 推送给双方用户
     */
    public void processChatMessage(ChatMessageEvent messageEvent) throws IOException {
        Long fromUserId = messageEvent.getFromUserId();
        Long toUserId = messageEvent.getToUserId();
        Long sessionId = messageEvent.getSessionId();
        Long messageId = messageEvent.getMessageId();

        log.info("📢 处理会话消息: 发送方={}, 接收方={}, sessionId={}", fromUserId, toUserId, sessionId);

        // 🔥 关键修改：检查接收方是否在当前聊天会话页面
        boolean isReceiverInChatSession = isUserInChatSession(toUserId, sessionId);
        Long messageStatus = isReceiverInChatSession ? 1L : 2L; // 1已读，2已送达

        // 构建消息数据
        Map<String, Object> messageData = Map.of(
                "id", messageId,
                "sessionId", sessionId,
                "fromUid", fromUserId,
                "toUid", toUserId,
                "content", messageEvent.getContent(),
                "status", messageStatus, // 动态设置状态
                "createTime", messageEvent.getCreateTime()
        );

        // 🔥 推送给发送方（更新消息列表）
        if (chatWebSocketHandler.isUserOnline(fromUserId)) {
            chatWebSocketHandler.sendMessageToUser(fromUserId, "NEW_MESSAGE", messageData);
            log.info("✅ NEW_MESSAGE 已推送给发送方: {}", fromUserId);
        } else {
            log.info("发送方 {} 离线", fromUserId);
        }

        // 🔥 推送给接收方（私聊页面 + 消息列表）
        if (chatWebSocketHandler.isUserOnline(toUserId)) {
            chatWebSocketHandler.sendMessageToUser(toUserId, "NEW_MESSAGE", messageData);
            log.info("✅ NEW_MESSAGE 已推送给接收方: {}", toUserId);

            // 更新消息状态
            ChatMessages message = new ChatMessages();
            message.setId(messageId);
            message.setStatus(messageStatus);
            chatMessagesService.updateById(message);

            log.info("📝 消息状态更新为: {}", isReceiverInChatSession ? "已读" : "已送达");
            // 🔥 新增：如果消息状态是已读，需要通知发送方更新状态
            if (isReceiverInChatSession) {
                notifySenderMessageRead(fromUserId, messageId, sessionId);
            }
        } else {
            log.info("接收方 {} 离线，消息保留在会话队列中", toUserId);
            // 离线用户默认设置为已送达状态
            ChatMessages message = new ChatMessages();
            message.setId(messageId);
            message.setStatus(2L); // 离线用户设为已送达
            chatMessagesService.updateById(message);
        }

        log.info("🎉 会话消息处理完成: sessionId={}", sessionId);
    }

    /**
     * 检查用户是否在指定的聊天会话页面
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return true-用户在会话页面，false-用户不在会话页面
     */
    private boolean isUserInChatSession(Long userId, Long sessionId) {
        return chatWebSocketHandler.isUserInChatSession(userId, sessionId);
    }
    /**
     * 🔥 新增：通知发送方消息已读
     */
    private void notifySenderMessageRead(Long fromUserId, Long messageId, Long sessionId) {
        try {
            if (chatWebSocketHandler.isUserOnline(fromUserId)) {
                Map<String, Object> readNotification = Map.of(
                        "type", "MESSAGE_READ",
                        "messageId", messageId,
                        "sessionId", sessionId,
                        "status", 1L // 已读状态
                );
                chatWebSocketHandler.sendMessageToUser(fromUserId, "MESSAGE_STATUS_UPDATE", readNotification);
                log.info("✅ 已通知发送方 {} 消息 {} 已读", fromUserId, messageId);
            }
        } catch (IOException e) {
            log.error("通知发送方消息已读失败", e);
        }
    }
}