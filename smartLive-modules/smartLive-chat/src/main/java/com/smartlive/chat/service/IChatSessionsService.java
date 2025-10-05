package com.smartlive.chat.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlive.chat.domain.ChatSessions;

/**
 * 私聊会话Service接口
 * 
 * @author 木木林
 * @date 2025-10-05
 */
public interface IChatSessionsService extends IService<ChatSessions>
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
     * 新增私聊会话 返回会话id
     * 
     * @param chatSessions 私聊会话
     * @return 结果
     */
    public Long insertChatSessions(ChatSessions chatSessions);

    /**
     * 修改私聊会话
     * 
     * @param chatSessions 私聊会话
     * @return 结果
     */
    public int updateChatSessions(ChatSessions chatSessions);

    /**
     * 批量删除私聊会话
     * 
     * @param ids 需要删除的私聊会话主键集合
     * @return 结果
     */
    public int deleteChatSessionsByIds(Long[] ids);

    /**
     * 删除私聊会话信息
     * 
     * @param id 私聊会话主键
     * @return 结果
     */
    public int deleteChatSessionsById(Long id);

    /**
     * 获取会话id
     * @param chatSessions
     * @return
     */
    Long getSessionId(ChatSessions chatSessions);
}
