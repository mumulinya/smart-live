package com.smartlive.chat.service.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartlive.chat.domain.ChatSessions;
import com.smartlive.chat.handle.ChatWebSocketHandler;
import com.smartlive.chat.service.IChatMessagesService;
import com.smartlive.chat.service.IChatSessionsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.smartlive.chat.mapper.ChatMessagesMapper;
import com.smartlive.chat.domain.ChatMessages;

/**
 * 用户聊天消息Service业务层处理
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@Service
@Slf4j
@Lazy
public class ChatMessagesServiceImpl extends ServiceImpl<ChatMessagesMapper,ChatMessages> implements IChatMessagesService
{
    @Autowired
    private ChatMessagesMapper chatMessagesMapper;

    private final IChatSessionsService chatSessionsService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    // 使用懒加载防止循环依赖
    public ChatMessagesServiceImpl(@Lazy IChatSessionsService chatSessionsService,@Lazy ChatWebSocketHandler chatWebSocketHandle) {
        this.chatSessionsService = chatSessionsService;
        this.chatWebSocketHandler = chatWebSocketHandle;
    }

    /**
     * 查询用户聊天消息
     * 
     * @param id 用户聊天消息主键
     * @return 用户聊天消息
     */
    @Override
    public ChatMessages selectChatMessagesById(Long id)
    {
        return chatMessagesMapper.selectChatMessagesById(id);
    }

    /**
     * 查询用户聊天消息列表
     * 
     * @param chatMessages 用户聊天消息
     * @return 用户聊天消息
     */
    @Override
    public List<ChatMessages> selectChatMessagesList(ChatMessages chatMessages,Integer current)
    {
        Long userId = UserContextHolder.getUser().getId();
        // 这里需要根据你的会话表结构来获取对方用户ID
        Long fromUserId = getOtherUserIdFromSession(chatMessages.getSessionId(), userId);
        Page<ChatMessages> page = query()
                .eq("session_id",chatMessages.getSessionId())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<ChatMessages> chatMessagesList = page.getRecords();
        // 判断列表中是否有未读消息
        boolean hasUnreadMessages = chatMessagesList.stream()
                .anyMatch(msg -> msg.getStatus() == 2&& msg.getToUid().equals(userId));
        // 反转列表，让最早的消息在前（为了前端显示）
        Collections.reverse(chatMessagesList);
        if(hasUnreadMessages){
            //设置消息为已读
            update()
                    .set("status",1)
                    .eq("session_id",chatMessages.getSessionId())
                    .eq("to_uid",userId)
                    .update();
            // 3. 通知对方用户
            if ( fromUserId != null) {
                batchNotifyMessagesRead(chatMessages.getSessionId(), userId, fromUserId);
            }
        }
        return chatMessagesList;
    }

    /**
     * 🔥 批量通知发送方所有消息已读
     * @param sessionId 会话ID
     * @param currentUserId 当前用户ID（阅读者）
     * @param fromUserId 发送方用户ID
     */
    private void batchNotifyMessagesRead(Long sessionId, Long currentUserId, Long fromUserId) {
        try {
            if (chatWebSocketHandler.isUserOnline(fromUserId)) {
                Map<String, Object> batchReadNotification = Map.of(
                        "type", "BATCH_MESSAGES_READ",
                        "sessionId", sessionId,
                        "readerUserId", currentUserId,
                        "status", 1L,
                        "timestamp", System.currentTimeMillis()
                );

                chatWebSocketHandler.sendMessageToUser(fromUserId, "MESSAGE_STATUS_UPDATE", batchReadNotification);
                log.info("✅ 已批量通知发送方 {} 会话 {} 的所有消息已被用户 {} 阅读",
                        fromUserId, sessionId, currentUserId);
            }
        } catch (IOException e) {
            log.error("批量通知发送方消息已读失败", e);
        }
    }
    /**
     * 从会话中获取对方用户ID
     */
    private Long getOtherUserIdFromSession(Long sessionId, Long currentUserId) {
         ChatSessions session = chatSessionsService.getById(sessionId);
         if (session.getMaxUserId().equals(currentUserId)) {
             return session.getLowUserId();
         } else {
             return session.getMaxUserId();
         }
    }
    /**
     * 新增用户聊天消息
     * 
     * @param chatMessages 用户聊天消息
     * @return 结果
     */
    @Override
    public int insertChatMessages(ChatMessages chatMessages)
    {
        chatMessages.setCreateTime(DateUtils.getNowDate());
        return chatMessagesMapper.insertChatMessages(chatMessages);
    }

    /**
     * 修改用户聊天消息
     * 
     * @param chatMessages 用户聊天消息
     * @return 结果
     */
    @Override
    public int updateChatMessages(ChatMessages chatMessages)
    {
        return chatMessagesMapper.updateChatMessages(chatMessages);
    }

    /**
     * 批量删除用户聊天消息
     * 
     * @param ids 需要删除的用户聊天消息主键
     * @return 结果
     */
    @Override
    public int deleteChatMessagesByIds(Long[] ids)
    {
        return chatMessagesMapper.deleteChatMessagesByIds(ids);
    }

    /**
     * 删除用户聊天消息信息
     * 
     * @param id 用户聊天消息主键
     * @return 结果
     */
    @Override
    public int deleteChatMessagesById(Long id)
    {
        return chatMessagesMapper.deleteChatMessagesById(id);
    }



}
