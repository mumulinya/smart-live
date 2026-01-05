package com.smartlive.chat.handle;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.dto.ChatMessageEvent;
import com.smartlive.chat.service.IChatMessagesService;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty 版本的聊天处理器
 * 注解 @ChannelHandler.Sharable 表示这个 Bean 可以被多个连接共享
 */
@Slf4j
@Component
@ChannelHandler.Sharable 
public class NettyChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IChatMessagesService chatMessagesService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 1. 管理所有连接的 ChannelGroup (自带线程安全)
    public static final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 2. 存储 用户ID -> Channel 的映射
    private static final Map<Long, Channel> userChannels = new ConcurrentHashMap<>();

    // 3. 存储 用户ID -> 当前活跃会话ID (你的业务逻辑)
    private static final Map<Long, Long> userActiveSessions = new ConcurrentHashMap<>();

    // 定义 Netty 中的 AttributeKey，用来替代 session.getAttributes()
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
            userActiveSessions.remove(userId);
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String payload = msg.text();
        log.info("📨 收到消息: {}", payload);

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
            } else {
                log.warn("未知消息类型: {}", type);
                sendErrorMessage(ctx.channel(), "未知的消息类型");
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendErrorMessage(ctx.channel(), "消息处理失败");
        }
    }

    // ========== 业务逻辑 (完美复刻你原来的逻辑) ==========

    private void handleAuthMessage(Channel channel, Map<String, Object> data) throws Exception {
        String token = (String) data.get("token");
        Object sessionIdObj = data.get("sessionId");
        Long sessionId = parseLong(sessionIdObj);

        log.info("🔐 处理身份认证, token: {}，用户当前的会话：{}", token, sessionId);

        try {
            String key = RedisConstants.LOGIN_USER_KEY + token;
            // 注意：这里假设 Redis 里存的是 Hash 结构，如果报错需检查 Redis 存储格式
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
            
            // 简单的判空保护
            if (userMap == null || userMap.isEmpty()) {
                sendAuthFailed(channel, "Token无效或已过期");
                return;
            }
            
            UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

            if (userDTO != null && userDTO.getId() != null) {
                Long userId = userDTO.getId();
                
                if (sessionId != null) {
                    userActiveSessions.put(userId, sessionId);
                }
                
                // 保存映射关系
                userChannels.put(userId, channel);
                // 绑定属性到 Channel
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

        log.info("💬 用户 {} 在会话 {} 发送消息给 {}: {}", fromUserId, sessionId, toUserId, content);

        // 1. 保存消息到数据库
        ChatMessages chatMessage = new ChatMessages();
        chatMessage.setFromUid(fromUserId);
        chatMessage.setToUid(toUserId);
        chatMessage.setContent(content);
        chatMessage.setSessionId(sessionId);
        chatMessage.setStatus(2L);
        chatMessage.setCreateTime(new Date());

        boolean saveResult = chatMessagesService.save(chatMessage);

        if (saveResult) {
            // 2. 发送成功确认
            sendMessage(channel, "MESSAGE_SENT", Map.of(
                    "tempId", tempId != null ? tempId : "",
                    "messageId", chatMessage.getId()
            ));

            // 3. 发送 MQ
            ChatMessageEvent messageEvent = new ChatMessageEvent();
            messageEvent.setType("CHAT_MESSAGE");
            messageEvent.setFromUserId(fromUserId);
            messageEvent.setToUserId(toUserId);
            messageEvent.setContent(content);
            messageEvent.setTempId(tempId);
            messageEvent.setSessionId(sessionId);
            messageEvent.setMessageId(chatMessage.getId());
            messageEvent.setCreateTime(new Date());

            String routingKey = MqConstants.CHAT_MESSAGE_ROUTING + sessionId;
            
            // 你的 MQ 工具类调用
            MqMessageSendUtils.sendMqMessage(
                rabbitTemplate,
                MqConstants.CHAT_EXCHANGE_NAME,
                routingKey,
                messageEvent,
                MqConstants.DEAD_LETTER_EXCHANGE_NAME, 
                MqConstants.DEAD_LETTER_ROUTING,
                3
            );
            log.info("✅ 消息已发送到MQ，sessionId: {}", sessionId);
        } else {
            sendErrorMessage(channel, "消息保存失败");
        }
    }
    
    private void handleUpdateActiveSession(Channel channel, Map<String, Object> data) {
        if (!isAuthenticated(channel)) return;
        Long userId = channel.attr(USER_ID_KEY).get();
        Long sessionId = parseLong(data.get("sessionId"));
        
        if (sessionId != null) {
            userActiveSessions.put(userId, sessionId);
        } else {
            userActiveSessions.remove(userId);
        }
    }


    // ========== 辅助方法 ==========

    /**
     * 🔥【新增】发送消息给指定用户 (对外暴露的公共方法)
     * 供 Consumer 或 Service 调用
     */
    public void sendMessageToUser(Long userId, String type, Object data) {
        // 1. 从静态 Map 中获取用户的 Channel
        Channel channel = userChannels.get(userId);

        // 2. 检查连接是否活跃
        if (channel != null && channel.isActive()) {
            try {
                // 3. 构造消息体
                Map<String, Object> message = Map.of(
                        "type", type,
                        "data", data,
                        "timestamp", System.currentTimeMillis()
                );

                // 4. 转成 JSON 字符串 (使用注入的 objectMapper)
                String messageJson = objectMapper.writeValueAsString(message);

                // 5. 发送 (注意：Netty 使用 TextWebSocketFrame，不是 TextMessage)
                channel.writeAndFlush(new TextWebSocketFrame(messageJson));

                // log.info("已推送消息给用户: {}", userId);
            } catch (Exception e) {
                log.error("发送消息给用户 {} 失败", userId, e);
            }
        } else {
            // log.warn("用户 {} 不在线，跳过推送", userId);
        }
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

    // 安全的类型转换工具
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
    
    // 对外暴露的静态方法，供 Controller 调用发送消息
    public static void pushMessageToUser(Long userId, String jsonMsg) {
        Channel channel = userChannels.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(jsonMsg));
        }
    }

    /**
     * 静态方法：检查用户是否在线
     */
    public static boolean isUserOnline(Long userId) {
        return userChannels.containsKey(userId);
    }

    /**
     * 静态方法：检查用户是否正在当前的会话窗口
     */
    public static boolean isUserInChatSession(Long userId, Long sessionId) {
        Long activeSessionId = userActiveSessions.get(userId);
        return activeSessionId != null && activeSessionId.equals(sessionId);
    }
}