package com.smartlive.chat.handle;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.utils.MqMessageSendUtils;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.dto.ChatMessageEvent;
import com.smartlive.chat.service.IChatMessagesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 整合 RabbitMQ 的聊天 WebSocket 处理器
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final IChatMessagesService chatMessagesService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 存储用户ID和WebSocket会话的映射
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 🔥 简化：只存储用户当前活跃的会话ID，不需要频繁的进入/离开
    private final Map<Long, Long> userActiveSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper, IChatMessagesService chatMessagesService) {
        this.objectMapper = objectMapper;
        this.chatMessagesService = chatMessagesService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🔗 WebSocket连接建立: {}", session.getId());
        sendSystemMessage(session, "连接成功");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("📨 收到消息: {}", payload);

        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");

            if ("AUTH".equals(type)) {
                handleAuthMessage(session, data);
            } else if ("CHAT_MESSAGE".equals(type)) {
                handleChatMessage(session, data);
            } else if ("UPDATE_ACTIVE_SESSION".equals(type)) {
                // 🔥 简化：只更新用户当前活跃会话
                handleUpdateActiveSession(session, data);
            } else {
                log.warn("未知消息类型: {}", type);
                sendErrorMessage(session, "未知的消息类型");
            }

        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendErrorMessage(session, "消息处理失败");
        }
    }

    /**
     * 处理身份认证
     */
    private void handleAuthMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        String token = (String) data.get("token");
        String sessionIdStr= (String) data.get("sessionId");
        Long sessionId=null;
        if (sessionIdStr!=null){
             sessionId= Long.parseLong(sessionIdStr);
        }
        log.info("🔐 处理身份认证, token: {}，用户当前的会话：{}", token,sessionId);

        try {
            // 验证token
            String key = RedisConstants.LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
            UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

            if (userDTO != null) {
                Long userId = userDTO.getId();
                if (sessionId != null&&userId != null) {
                    // 存储用户当前会话ID
                    userActiveSessions.put(userId, sessionId);
                    log.info("👀 用户 {} 当前活跃会话: {}", userId, sessionId);
                }
                // 存储用户会话映射
                userSessions.put(userId, session);
                session.getAttributes().put("userId", userId);
                session.getAttributes().put("authenticated", true);

                // 发送认证成功消息
                sendAuthSuccess(session, userId);
                log.info("✅ 用户 {} 认证成功", userId);
            } else {
                sendAuthFailed(session, "Token无效或已过期");
                log.warn("❌ Token认证失败");
            }

        } catch (Exception e) {
            log.error("认证处理异常", e);
            sendAuthFailed(session, "认证处理异常");
        }
    }

    /**
     * 🔥 简化：处理更新用户活跃会话
     */
    private void handleUpdateActiveSession(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (!isAuthenticated(session)) {
            return;
        }

        Long userId = getUserIdFromSession(session);
        Long sessionId = parseSessionId(data.get("sessionId"));

        if (sessionId != null) {
            // 更新用户当前活跃的会话
            userActiveSessions.put(userId, sessionId);
            log.info("👀 用户 {} 当前活跃会话: {}", userId, sessionId);
        } else {
            // sessionId为null表示用户没有活跃会话
            userActiveSessions.remove(userId);
            log.info("🚪 用户 {} 无活跃会话", userId);
        }
    }

    /**
     * 处理聊天消息 - 使用sessionId作为队列名称
     */
    private void handleChatMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        // 检查认证状态
        if (!isAuthenticated(session)) {
            sendErrorMessage(session, "未认证，请先进行身份认证");
            return;
        }

        Long fromUserId = getUserIdFromSession(session);
        Long toUserId = ((Number) data.get("toUserId")).longValue();
        String content = (String) data.get("content");
        String tempId = (String) data.get("tempId");
        Long sessionId = parseSessionId(data.get("sessionId"));

        if (sessionId == null) {
            sendErrorMessage(session, "sessionId不能为空");
            return;
        }

        log.info("💬 用户 {} 在会话 {} 发送消息给 {}: {}", fromUserId, sessionId, toUserId, content);

        try {
            // 1. 保存消息到数据库
            ChatMessages chatMessage = new ChatMessages();
            chatMessage.setFromUid(fromUserId);
            chatMessage.setToUid(toUserId);
            chatMessage.setContent(content);
            chatMessage.setSessionId(sessionId);
            chatMessage.setStatus(2L); // 2-已发送
            chatMessage.setCreateTime(new Date());

            boolean saveResult = chatMessagesService.save(chatMessage);

            if (saveResult) {
                // 2. 发送成功确认给发送者
                sendMessage(session, "MESSAGE_SENT", Map.of(
                        "tempId", tempId,
                        "messageId", chatMessage.getId()
                ));

                // 3. 创建消息事件并发送到会话队列
                ChatMessageEvent messageEvent = new ChatMessageEvent();
                messageEvent.setType("CHAT_MESSAGE");
                messageEvent.setFromUserId(fromUserId);
                messageEvent.setToUserId(toUserId);
                messageEvent.setContent(content);
                messageEvent.setTempId(tempId);
                messageEvent.setSessionId(sessionId);
                messageEvent.setMessageId(chatMessage.getId());
                messageEvent.setCreateTime(new Date());
//                //创建correlationData
//                CorrelationData cd=new CorrelationData(UUID.randomUUID().toString());
//                cd.getFuture().addCallback(new ListenableFutureCallback<>() {
//                    @Override
//                    public void onFailure(Throwable ex) {
//                        log.error("spring amqp 处理确认结果异常 ：{}", ex);
//                    }
//
//                    @Override
//                    public void onSuccess(CorrelationData.Confirm result) {
//                        //判断是否成功
//                        if (result.isAck()) {
//                            log.info("✅ 消息已确认，sessionId: {}", sessionId);
//                        } else {
//                            log.error("❌ 消息发送失败，sessionId: {},错误原因为：{}", sessionId, result.getReason());
//                        }
//                    }
//                });
//                // 发送到会话队列
                String routingKey = MqConstants.CHAT_MESSAGE_ROUTING + sessionId;
//                rabbitTemplate.convertAndSend("session.chat.topic", routingKey, messageEvent,cd);
//                //使用消息重发机制发送消息
                MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.CHAT_EXCHANGE_NAME,routingKey, messageEvent);
                log.info("✅ 消息已发送到会话队列，sessionId: {}", sessionId);

            } else {
                sendErrorMessage(session, "消息保存失败");
                log.error("❌ 消息保存失败");
            }

        } catch (Exception e) {
            log.error("处理聊天消息异常", e);
            sendErrorMessage(session, "消息处理异常");
        }
    }

    // ========== 消息发送方法 ==========

    /**
     * 发送消息给指定用户
     */
    public void sendMessageToUser(Long userId, String type, Object data) throws IOException {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            Map<String, Object> message = Map.of(
                    "type", type,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            );
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
        }
    }

    /**
     * 发送系统消息
     */
    private void sendSystemMessage(WebSocketSession session, String content) throws IOException {
        sendMessage(session, "SYSTEM_MESSAGE", Map.of("content", content));
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String error) throws IOException {
        sendMessage(session, "ERROR", Map.of("error", error));
    }

    /**
     * 发送认证成功消息
     */
    private void sendAuthSuccess(WebSocketSession session, Long userId) throws IOException {
        sendMessage(session, "AUTH_SUCCESS", Map.of("userId", userId));
    }

    /**
     * 发送认证失败消息
     */
    private void sendAuthFailed(WebSocketSession session, String reason) throws IOException {
        sendMessage(session, "AUTH_FAILED", Map.of("reason", reason));
    }

    /**
     * 通用消息发送方法
     */
    private void sendMessage(WebSocketSession session, String type, Object data) throws IOException {
        Map<String, Object> message = Map.of(
                "type", type,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        String messageJson = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(messageJson));
    }

    // ========== 工具方法 ==========

    private boolean isAuthenticated(WebSocketSession session) {
        Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
        return authenticated != null && authenticated;
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }

    private Long parseSessionId(Object sessionIdObj) {
        if (sessionIdObj == null) return null;

        try {
            if (sessionIdObj instanceof String) {
                return Long.parseLong((String) sessionIdObj);
            } else if (sessionIdObj instanceof Number) {
                return ((Number) sessionIdObj).longValue();
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid sessionId format: {}", sessionIdObj);
        }
        return null;
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserIdFromSession(session);
        log.error("❌ WebSocket传输错误, userId: {}", userId, exception);
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        log.info("🔌 WebSocket连接关闭: {}, userId: {}, status: {}", session.getId(), userId, status);
        cleanupSession(session);
    }

    /**
     * 清理会话资源
     */
    private void cleanupSession(WebSocketSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            userActiveSessions.remove(userId);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 🔥 简化：检查用户是否在指定的聊天会话页面
     */
    public boolean isUserInChatSession(Long userId, Long sessionId) {
        Long activeSession = userActiveSessions.get(userId);
        log.info("🔥 检查用户在哪个会话: activeSession={}, sessionId={}", activeSession, sessionId);
        return activeSession != null && activeSession.equals(sessionId);
    }
}