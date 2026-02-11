package com.smartlive.im.handler;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.chat.api.RemoteChatService;
import com.smartLive.chat.api.dto.ChatMessageDTO;
import com.smartLive.chat.api.dto.ChatMessageEvent;
import com.smartLive.chat.api.dto.UserSessionDTO;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty 版本的聊天处理器 (Refactored for smart-live-im with Redis Sync)
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RemoteChatService remoteChatService;

    @Autowired
    private RedisService redisService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Map<Long, Channel> userChannels = new ConcurrentHashMap<>();

    // Redis Keys
    private static final String IM_ONLINE_KEY = "im:online:";
    private static final String IM_SESSION_KEY = "im:session:";

    private static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");
    private static final AttributeKey<Boolean> AUTH_KEY = AttributeKey.valueOf("authenticated");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("🔗 Netty 连接建立: {}", ctx.channel().id());
        allChannels.add(ctx.channel());
        sendSystemMessage(ctx.channel(), "连接成功");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long userId = ctx.channel().attr(USER_ID_KEY).get();
        log.info("🔌 Netty 连接关闭: {}, userId: {}", ctx.channel().id(), userId);
        if (userId != null) {
            userChannels.remove(userId);
            // Clear Redis State
            redisService.deleteObject(IM_ONLINE_KEY + userId);
            redisService.deleteObject(IM_SESSION_KEY + userId);
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String payload = msg.text();
        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");

            if ("AUTH".equals(type)) {
                handleAuthMessage(ctx.channel(), data);
            } else if ("CHAT_MESSAGE".equals(type)) {
                handleChatMessage(ctx.channel(), data);
            } else if ("UPDATE_ACTIVE_SESSION".equals(type)) {
                handleUpdateActiveSession(ctx.channel(), data);
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendErrorMessage(ctx.channel(), "消息处理失败");
        }
    }

    private void handleAuthMessage(Channel channel, Map<String, Object> data) throws Exception {
        String token = (String) data.get("token");
        Object sessionIdObj = data.get("sessionId");
        Long sessionId = parseLong(sessionIdObj);

        try {
            String key = RedisConstants.LOGIN_USER_KEY + token;
            Map<String, Object> userMap = redisService.getCacheMap(key);

            if (userMap == null || userMap.isEmpty()) {
                sendAuthFailed(channel, "Token无效或已过期");
                return;
            }

            UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

            if (userDTO != null && userDTO.getId() != null) {
                Long userId = userDTO.getId();

                if (sessionId != null) {
                    redisService.setCacheObject(IM_SESSION_KEY + userId, sessionId, 24L, TimeUnit.HOURS);
                }

                // Set Online Status in Redis
                redisService.setCacheObject(IM_ONLINE_KEY + userId, true, 24L, TimeUnit.HOURS);

                userChannels.put(userId, channel);
                channel.attr(USER_ID_KEY).set(userId);
                channel.attr(AUTH_KEY).set(true);

                sendAuthSuccess(channel, userId);
                log.info("✅ 用户 {} 认证成功", userId);
            } else {
                sendAuthFailed(channel, "Token无效");
            }
        } catch (Exception e) {
            log.error("认证处理异常", e);
            sendAuthFailed(channel, "认证处理异常");
        }
    }

    private void handleChatMessage(Channel channel, Map<String, Object> data) throws Exception {
        if (!isAuthenticated(channel)) {
            sendErrorMessage(channel, "未认证，请先进行身份认证");
            return;
        }

        Long fromUserId = channel.attr(USER_ID_KEY).get();
        Long toUserId = parseLong(data.get("toUserId"));
        String content = (String) data.get("content");
        String tempId = (String) data.get("tempId");
        Long sessionId = parseLong(data.get("sessionId"));

        if (sessionId == null) {
            sendErrorMessage(channel, "sessionId不能为空");
            return;
        }

        // 1. Save via Feign
        ChatMessageDTO chatMessage = new ChatMessageDTO();
        chatMessage.setFromUid(fromUserId);
        chatMessage.setToUid(toUserId);
        chatMessage.setContent(content);
        chatMessage.setSessionId(sessionId);
        chatMessage.setStatus(2L);
        chatMessage.setCreateTime(new Date());

        boolean saveSuccess = false;
        Long messageId = null;
        try {
            messageId = remoteChatService.saveMessage(chatMessage);
            if (messageId != null && messageId > 0) {
                saveSuccess = true;
            }
        } catch (Exception e) {
            log.error("远程保存消息失败", e);
        }

        if (saveSuccess) {
            // 2. Sync Session
            try {
                UserSessionDTO session1 = new UserSessionDTO();
                session1.setUserId(toUserId);
                session1.setTargetUid(fromUserId);
                session1.setSessionId(sessionId);
                remoteChatService.syncSession(session1);

                UserSessionDTO session2 = new UserSessionDTO();
                session2.setUserId(fromUserId);
                session2.setTargetUid(toUserId);
                session2.setSessionId(sessionId);
                remoteChatService.syncSession(session2);
            } catch (Exception e) {
                log.error("远程同步会话失败", e);
            }

            // 3. Ack
            sendMessage(channel, "MESSAGE_SENT", Map.of(
                    "tempId", tempId != null ? tempId : "",
                    "messageId", 0L // ID generation handled by DB, might need to return it
            ));

            // 4. Send MQ
            ChatMessageEvent messageEvent = new ChatMessageEvent();
            messageEvent.setType("CHAT_MESSAGE");
            messageEvent.setFromUserId(fromUserId);
            messageEvent.setToUserId(toUserId);
            messageEvent.setContent(content);
            messageEvent.setTempId(tempId);
            messageEvent.setSessionId(sessionId);
            messageEvent.setMessageId(messageId);
            messageEvent.setCreateTime(new Date());

            String routingKey = MqConstants.CHAT_MESSAGE_ROUTING + sessionId;

            MqMessageSendUtils.sendMqMessage(
                rabbitTemplate,
                MqConstants.CHAT_EXCHANGE_NAME,
                routingKey,
                    messageEvent, // 发送 JSON 字符串
                MqConstants.DEAD_LETTER_EXCHANGE_NAME,
                MqConstants.DEAD_LETTER_ROUTING,
                3
            );
        } else {
            sendErrorMessage(channel, "消息保存失败");
        }
    }

    private void handleUpdateActiveSession(Channel channel, Map<String, Object> data) {
        if (!isAuthenticated(channel)) return;
        Long userId = channel.attr(USER_ID_KEY).get();
        Long sessionId = parseLong(data.get("sessionId"));

        if (sessionId != null) {
            redisService.setCacheObject(IM_SESSION_KEY + userId, sessionId, 24L, TimeUnit.HOURS);
        } else {
            redisService.deleteObject(IM_SESSION_KEY + userId);
        }
    }

    public static void pushMessageToUser(Long userId, String jsonMsg) {
        Channel channel = userChannels.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(jsonMsg));
        }
    }

    public static boolean isUserOnline(Long userId) {
        return userChannels.containsKey(userId);
    }

    private boolean isAuthenticated(Channel channel) {
        Boolean auth = channel.attr(AUTH_KEY).get();
        return auth != null && auth;
    }

    private void sendMessage(Channel channel, String type, Object data) throws Exception {
        Map<String, Object> message = Map.of(
                "type", type,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        String json = objectMapper.writeValueAsString(message);
        channel.writeAndFlush(new TextWebSocketFrame(json));
    }

    private void sendErrorMessage(Channel channel, String error) throws Exception {
        sendMessage(channel, "ERROR", Map.of("error", error));
    }

    private void sendSystemMessage(Channel channel, String content) throws Exception {
        sendMessage(channel, "SYSTEM_MESSAGE", Map.of("content", content));
    }

    private void sendAuthSuccess(Channel channel, Long userId) throws Exception {
        sendMessage(channel, "AUTH_SUCCESS", Map.of("userId", userId));
    }

    private void sendAuthFailed(Channel channel, String reason) throws Exception {
        sendMessage(channel, "AUTH_FAILED", Map.of("reason", reason));
    }

    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
