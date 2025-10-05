package com.smartlive.chat.service.impl;

import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartlive.chat.domain.UserSessions;
import com.smartlive.chat.service.IChatMessagesService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ChatMessagesServiceImpl extends ServiceImpl<ChatMessagesMapper,ChatMessages> implements IChatMessagesService
{
    @Autowired
    private ChatMessagesMapper chatMessagesMapper;

    @Autowired
    private RemoteAppUserService remoteAppUserService;

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
        Page<ChatMessages> page = query()
                .eq("session_id",chatMessages.getSessionId())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<ChatMessages> chatMessagesList = page.getRecords();
        // 反转列表，让最早的消息在前（为了前端显示）
        Collections.reverse(chatMessagesList);
        if(chatMessagesList.size() > 0){
            //设置消息为已读
            update()
                    .set("status",1)
                    .eq("session_id",chatMessages.getSessionId())
                    .eq("to_uid",userId)
                    .update();
        }
        return chatMessagesList;
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
