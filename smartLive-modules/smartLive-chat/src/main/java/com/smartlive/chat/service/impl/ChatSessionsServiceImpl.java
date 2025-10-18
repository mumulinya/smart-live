package com.smartlive.chat.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.User;
import com.smartlive.chat.domain.UserSessions;
import com.smartlive.chat.mapper.UserSessionsMapper;
import com.smartlive.chat.service.IUserSessionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartlive.chat.mapper.ChatSessionsMapper;
import com.smartlive.chat.domain.ChatSessions;
import com.smartlive.chat.service.IChatSessionsService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 私聊会话Service业务层处理
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@Service
public class ChatSessionsServiceImpl extends ServiceImpl<ChatSessionsMapper, ChatSessions> implements IChatSessionsService
{
    @Autowired
    private ChatSessionsMapper chatSessionsMapper;
    @Autowired
    private RemoteAppUserService remoteAppUserService;
    private final IUserSessionsService userSessionsService;

    public ChatSessionsServiceImpl(IUserSessionsService userSessionsService) {
        this.userSessionsService = userSessionsService;
    }
    /**
     * 查询私聊会话
     * 
     * @param id 私聊会话主键
     * @return 私聊会话
     */
    @Override
    public ChatSessions selectChatSessionsById(Long id)
    {
        ChatSessions chatSessions = chatSessionsMapper.selectChatSessionsById(id);
        Long userId = UserContextHolder.getUser().getId();
        //获取发送消息和接收消息的uid
        if(chatSessions.getMaxUserId().equals(userId)){
            chatSessions.setFromUid(chatSessions.getMaxUserId());
            chatSessions.setToUid(chatSessions.getLowUserId());
        }else{
            chatSessions.setFromUid(chatSessions.getLowUserId());
            chatSessions.setToUid(chatSessions.getMaxUserId());
        }
        //获取接收消息用户的个人信息
        R<User> user = remoteAppUserService.queryUserById(chatSessions.getToUid());
        chatSessions.setContactName(user.getData().getNickName());
        chatSessions.setContactAvatar(user.getData().getIcon());
        return chatSessions;
    }

    /**
     * 查询私聊会话列表
     * 
     * @param chatSessions 私聊会话
     * @return 私聊会话
     */
    @Override
    public List<ChatSessions> selectChatSessionsList(ChatSessions chatSessions)
    {
        return chatSessionsMapper.selectChatSessionsList(chatSessions);
    }

    /**
     * 新增私聊会话
     * 
     * @param chatSessions 私聊会话
     * @return 结果
     */
    @Override
    @Transactional
    public Long insertChatSessions(ChatSessions chatSessions)
    {
        chatSessions.setCreateTime(DateUtils.getNowDate());
        boolean i = save(chatSessions);
        //会话创建成功，给每个用户新增用户会话
        if(i){
            //第一个用户会话
            UserSessions maxUserSessions = UserSessions.builder()
                    .sessionId(chatSessions.getId())
                    .userId(chatSessions.getMaxUserId())
                    .targetUid(chatSessions.getLowUserId())
                    .createTime(DateUtils.getNowDate())
                    .build();
            userSessionsService.save(maxUserSessions);
            //第二个用户会话
            UserSessions lowUserSessions = UserSessions.builder()
                    .sessionId(chatSessions.getId())
                    .userId(chatSessions.getLowUserId())
                    .targetUid(chatSessions.getMaxUserId())
                    .createTime(DateUtils.getNowDate())
                    .build();
                    userSessionsService.save(lowUserSessions);
        }
        return chatSessions.getId();
    }

    /**
     * 修改私聊会话
     * 
     * @param chatSessions 私聊会话
     * @return 结果
     */
    @Override
    public int updateChatSessions(ChatSessions chatSessions)
    {
        return chatSessionsMapper.updateChatSessions(chatSessions);
    }

    /**
     * 批量删除私聊会话
     * 
     * @param ids 需要删除的私聊会话主键
     * @return 结果
     */
    @Override
    public int deleteChatSessionsByIds(Long[] ids)
    {
        return chatSessionsMapper.deleteChatSessionsByIds(ids);
    }

    /**
     * 删除私聊会话信息
     * 
     * @param id 私聊会话主键
     * @return 结果
     */
    @Override
    public int deleteChatSessionsById(Long id)
    {
        return chatSessionsMapper.deleteChatSessionsById(id);
    }

    /**
     * 获取会话id
     *
     * @param chatSessions
     * @return
     */
    @Override
    public Long getSessionId(ChatSessions chatSessions) {
        Long sessionId = query().eq("max_user_id", chatSessions.getMaxUserId())
                .eq("low_user_id", chatSessions.getLowUserId())
                .one().getId();
        return sessionId;
    }
}
