package com.smartlive.chat.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlive.chat.domain.ChatSessions;

/**
 * 私聊会话Mapper接口
 * 
 * @author 木木林
 * @date 2025-10-05
 */
public interface ChatSessionsMapper extends BaseMapper<ChatSessions>
{
    /**
     * 查询私聊会话
     * 
     * @param id 私聊会话主键
     * @return 私聊会话
     */
    public ChatSessions selectChatSessionsById(Long id);

    /**
     * 查询私聊会话列表
     * 
     * @param chatSessions 私聊会话
     * @return 私聊会话集合
     */
    public List<ChatSessions> selectChatSessionsList(ChatSessions chatSessions);

    /**
     * 新增私聊会话
     * 
     * @param chatSessions 私聊会话
     * @return 结果
     */
    public int insertChatSessions(ChatSessions chatSessions);

    /**
     * 修改私聊会话
     * 
     * @param chatSessions 私聊会话
     * @return 结果
     */
    public int updateChatSessions(ChatSessions chatSessions);

    /**
     * 删除私聊会话
     * 
     * @param id 私聊会话主键
     * @return 结果
     */
    public int deleteChatSessionsById(Long id);

    /**
     * 批量删除私聊会话
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteChatSessionsByIds(Long[] ids);
}
