package com.smartlive.chat.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlive.chat.domain.ChatMessages;

/**
 * 用户聊天消息Mapper接口
 * 
 * @author 木木林
 * @date 2025-10-05
 */
public interface ChatMessagesMapper extends BaseMapper<ChatMessages>
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
    public List<ChatMessages> selectChatMessagesList(ChatMessages chatMessages);

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
     * 删除用户聊天消息
     * 
     * @param id 用户聊天消息主键
     * @return 结果
     */
    public int deleteChatMessagesById(Long id);

    /**
     * 批量删除用户聊天消息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteChatMessagesByIds(Long[] ids);
}
