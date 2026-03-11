package com.smartlive.im.handler;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.core.constant.mq.ChatMqConstants;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.chat.api.RemoteChatService;
import com.smartLive.chat.api.dto.ChatMessageDTO;
import com.smartLive.chat.api.dto.ChatMessageEvent;
import com.smartLive.chat.api.dto.UserSessionDTO;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.user.api.domain.UserDTO;
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
 * Netty 核心业务处理器 (WebSocket 消息分发枢纽)
 * 采用全异步非阻塞模型，处理客户端认证、点对点聊天实时发送、会话状态同步及 Redis 离线状态实时更新。
 * 集成了 RabbitMQ 广播消息至业务后端，保证消息持久化与多端分发。
 * 
 * @author smartLive
 * @date 2026-03-11
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
    private MqMessageSendUtils mqMessageSendUtils;

    /**
     * 管理所有活跃连接的 ChannelGroup，用于系统预警或全局广播
     */
    public static final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
    /**
     * 用户 ID 与 Netty 通道的映射容器，仅存储当前服务器承载的活跃用户连接
     */
    private static final Map<Long, Channel> userChannels = new ConcurrentHashMap<>();

    private static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");
    private static final AttributeKey<Boolean> AUTH_KEY = AttributeKey.valueOf("authenticated");

    /**
     * 通道激活事件：记录新连接接入
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("🔗 Netty 连接建立: {}", ctx.channel().id());
        allChannels.add(ctx.channel());
        sendSystemMessage(ctx.channel(), "连接成功");
    }

    /**
     * 通道失效事件：清理内存映射与 Redis 在线状态快照，防止僵尸连接占用资源
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long userId = ctx.channel().attr(USER_ID_KEY).get();
        log.info("🔌 Netty 连接关闭: {}, userId: {}", ctx.channel().id(), userId);
        if (userId != null) {
            userChannels.remove(userId);
            // 同步清理 Redis 中的在线状态，供其他集群节点查询
            redisService.deleteObject(RedisConstants.IM_ONLINE_KEY + userId);
            redisService.deleteObject(RedisConstants.IM_SESSION_KEY + userId);
        }
        super.channelInactive(ctx);
    }

    /**
     * 核心读消息处理入口：解析 WebSocket 文本帧并进行类型分发
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String payload = msg.text();
        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");

            if ("AUTH".equals(type)) {
                // 身份令牌认证
                handleAuthMessage(ctx.channel(), data);
            } else if ("CHAT_MESSAGE".equals(type)) {
                // 实时私聊消息
                handleChatMessage(ctx.channel(), data);
            } else if ("UPDATE_ACTIVE_SESSION".equals(type)) {
                // 前端会话窗口切换上报，用于精准推送未读
                handleUpdateActiveSession(ctx.channel(), data);
            }
        } catch (Exception e) {
            log.error("收到格式错误的消息或处理异常: {}", payload, e);
            sendErrorMessage(ctx.channel(), "非法消息格式");
        }
    }

    /**
     * 认证处理器：基于 Redis Token 换取用户信息并绑定连接
     */
    private void handleAuthMessage(Channel channel, Map<String, Object> data) throws Exception {
        String token = (String) data.get("token");
        Object sessionIdObj = data.get("sessionId");
        Long sessionId = parseLong(sessionIdObj);

        try {
            String key = RedisConstants.LOGIN_USER_KEY + token;
            Map<String, Object> userMap = redisService.getCacheMap(key);

            if (userMap == null || userMap.isEmpty()) {
                sendAuthFailed(channel, "登录已失效，请重新登录");
                return;
            }

            UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

            if (userDTO != null && userDTO.getId() != null) {
                Long userId = userDTO.getId();

                // 记录当前活跃会话至 Redis，支持离线统计
                if (sessionId != null) {
                    redisService.setCacheObject(RedisConstants.IM_SESSION_KEY + userId, sessionId, 24L, TimeUnit.HOURS);
                }

                // 标记全局在线状态快照
                redisService.setCacheObject(RedisConstants.IM_ONLINE_KEY + userId, true, 24L, TimeUnit.HOURS);

                // 绑定内存映射
                userChannels.put(userId, channel);
                channel.attr(USER_ID_KEY).set(userId);
                channel.attr(AUTH_KEY).set(true);

                sendAuthSuccess(channel, userId);
                log.info("✅ 用户 {} 认证成功并上线", userId);
            } else {
                sendAuthFailed(channel, "无效的用户凭证");
            }
        } catch (Exception e) {
            log.error("认证过程系统异常", e);
            sendAuthFailed(channel, "认证失败，系统繁忙");
        }
    }

    /**
     * 消息处理器：
     * 1. 业务保存：通过 Feign 调用 Chat 模块实现入库。
     * 2. 会话维护：异步触发双向会话窗口同步。
     * 3. 实时分发：通过 MQ 将消息投递到主题队列，触发布推（ImPushConsumer）。
     */
    private void handleChatMessage(Channel channel, Map<String, Object> data) throws Exception {
        if (!isAuthenticated(channel)) {
            sendErrorMessage(channel, "未认证连接");
            return;
        }

        Long fromUserId = channel.attr(USER_ID_KEY).get();
        Long toUserId = parseLong(data.get("toUserId"));
        String content = (String) data.get("content");
        String tempId = (String) data.get("tempId");
        Long sessionId = parseLong(data.get("sessionId"));

        if (sessionId == null) {
            sendErrorMessage(channel, "未指定有效的会话 ID");
            return;
        }

        // 1. 远程持久化
        ChatMessageDTO chatMessage = new ChatMessageDTO();
        chatMessage.setFromUid(fromUserId);
        chatMessage.setToUid(toUserId);
        chatMessage.setContent(content);
        chatMessage.setSessionId(sessionId);
        chatMessage.setStatus(2L); // 默认未读
        chatMessage.setCreateTime(new Date());

        boolean saveSuccess = false;
        Long messageId = null;
        try {
            messageId = remoteChatService.saveMessage(chatMessage);
            if (messageId != null && messageId > 0) {
                saveSuccess = true;
            }
        } catch (Exception e) {
            log.error("调用 Chat 模块保存消息异常", e);
        }

        if (saveSuccess) {
            // 2. 异步同步会话记录 (Feign 调用)
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
                log.error("异步同步会话联系人出错", e);
            }

            // 3. 回复客户端 Ack 信号，解除前端发送态
            sendMessage(channel, "MESSAGE_SENT", Map.of(
                    "tempId", tempId != null ? tempId : "",
                    "messageId", messageId
            ));

            // 4. 发送 RabbitMQ 事件，广播推送给接收方和其他端
            ChatMessageEvent messageEvent = new ChatMessageEvent();
            messageEvent.setType("CHAT_MESSAGE");
            messageEvent.setFromUserId(fromUserId);
            messageEvent.setToUserId(toUserId);
            messageEvent.setContent(content);
            messageEvent.setTempId(tempId);
            messageEvent.setSessionId(sessionId);
            messageEvent.setMessageId(messageId);
            messageEvent.setCreateTime(new Date());

            String routingKey = ChatMqConstants.CHAT_MESSAGE_ROUTING + sessionId;

            mqMessageSendUtils.sendMqMessage(
                ChatMqConstants.CHAT_DIRECT_EXCHANGE,
                routingKey,
                messageEvent, 
                 3
            );
        } else {
            sendErrorMessage(channel, "消息暂存失败，请检查网络");
        }
    }

    /**
     * 前端活跃会话标记：用于精准计算用户未读状态
     */
    private void handleUpdateActiveSession(Channel channel, Map<String, Object> data) {
        if (!isAuthenticated(channel)) return;
        Long userId = channel.attr(USER_ID_KEY).get();
        Long sessionId = parseLong(data.get("sessionId"));

        if (sessionId != null) {
            redisService.setCacheObject(RedisConstants.IM_SESSION_KEY + userId, sessionId, 24L, TimeUnit.HOURS);
        } else {
            redisService.deleteObject(RedisConstants.IM_SESSION_KEY + userId);
        }
    }

    /**
     * 通用单播推送：将消息定向写入 Netty 通道
     */
    public static void pushMessageToUser(Long userId, String jsonMsg) {
        Channel channel = userChannels.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(jsonMsg));
        }
    }

    /**
     * 判定本地是否承载该用户连接
     */
    public static boolean isUserOnline(Long userId) {
        return userChannels.containsKey(userId);
    }

    private boolean isAuthenticated(Channel channel) {
        Boolean auth = channel.attr(AUTH_KEY).get();
        return auth != null && auth;
    }

    /**
     * 构造并发送统一响应格式的 WebSocket 数据包
     */
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
