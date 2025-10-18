package com.smartlive.chat.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlive.chat.domain.ChatMessages;

/**
 * 用户聊天消息Service接口
 * 
 * @author 木木林
 * @date 2025-10-05
 */
public interface IChatMessagesService extends IService<ChatMessages>
{
    /**
     * 查询用户聊天消息
     * 
     * @param id 用户聊天消息主键
     * @return 用户聊天消息
     */
    public ChatMessages selectChatMessagesById(Long id);

    /**
     * 查询用户聊天消息列表
     * 
     * @param chatMessages 用户聊天消息
     * @return 用户聊天消息集合
     */
    public List<ChatMessages> selectChatMessagesList(ChatMessages chatMessages,Integer current);

    /**
     * 新增用户聊天消息
     * 
     * @param chatMessages 用户聊天消息
     * @return 结果
     */
    public int insertChatMessages(ChatMessages chatMessages);

    /**
     * 修改用户聊天消息
     * 
     * @param chatMessages 用户聊天消息
     * @return 结果
     */
    public int updateChatMessages(ChatMessages chatMessages);

    /**
     * 批量删除用户聊天消息
     * 
     * @param ids 需要删除的用户聊天消息主键集合
     * @return 结果
     */
    public int deleteChatMessagesByIds(Long[] ids);

    /**
     * 删除用户聊天消息信息
     * 
     * @param id 用户聊天消息主键
     * @return 结果
     */
    public int deleteChatMessagesById(Long id);

}
