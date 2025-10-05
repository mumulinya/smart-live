package com.smartlive.chat.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.User;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.service.IChatMessagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartlive.chat.mapper.UserSessionsMapper;
import com.smartlive.chat.domain.UserSessions;
import com.smartlive.chat.service.IUserSessionsService;

/**
 * 用户会话列表Service业务层处理
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@Service
public class UserSessionsServiceImpl  extends ServiceImpl<UserSessionsMapper, UserSessions> implements IUserSessionsService
{
    @Autowired
    private UserSessionsMapper userSessionsMapper;

    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Autowired
    private IChatMessagesService chatMessagesService;

    /**
     * 查询用户会话列表
     * 
     * @param id 用户会话列表主键
     * @return 用户会话列表
     */
    @Override
    public UserSessions selectUserSessionsById(Long id)
    {
        return userSessionsMapper.selectUserSessionsById(id);
    }

    /**
     * 查询用户会话列表列表
     * 
     * @param userSessions 用户会话列表
     * @return 用户会话列表
     */
    @Override
    public List<UserSessions> selectUserSessionsList(UserSessions userSessions,Integer current)
    {
        Page<UserSessions> page = query()
                .eq("user_id",userSessions.getUserId())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<UserSessions> userSessionsList = page.getRecords();
        userSessionsList.stream().forEach(c -> {
            //获取用户信息
            Long id = c.getTargetUid();
            R<User> user = remoteAppUserService.queryUserById(id);
            c.setNickname(user.getData().getNickName());
            c.setAvatar(user.getData().getIcon());
            //获取未读消息数量
            Long count = chatMessagesService.query()
                    .eq("session_id", c.getSessionId())
                    .eq("from_uid", c.getTargetUid())
                    .eq("to_uid", c.getUserId())
                    .eq("status", 2)
                    .count();
            c.setUnread(count.intValue());
            //获取最后一条消息
            ChatMessages chatMessages = chatMessagesService.query()
                    .eq("session_id", c.getSessionId())
                    .orderByDesc("create_time")
                    .last("limit 1")
                    .one();
            if(chatMessages!=null){
                if (chatMessages.getContent()!=null&&chatMessages.getContent()!=""){
                    c.setLastMessage(chatMessages.getContent());
                }
                c.setLastTime(chatMessages.getCreateTime());
            }
        });
        return userSessionsList;
    }

    /**
     * 新增用户会话列表
     * 
     * @param userSessions 用户会话列表
     * @return 结果
     */
    @Override
    public int insertUserSessions(UserSessions userSessions)
    {
        return userSessionsMapper.insertUserSessions(userSessions);
    }

    /**
     * 修改用户会话列表
     * 
     * @param userSessions 用户会话列表
     * @return 结果
     */
    @Override
    public int updateUserSessions(UserSessions userSessions)
    {
        return userSessionsMapper.updateUserSessions(userSessions);
    }

    /**
     * 批量删除用户会话列表
     * 
     * @param ids 需要删除的用户会话列表主键
     * @return 结果
     */
    @Override
    public int deleteUserSessionsByIds(Long[] ids)
    {
        return userSessionsMapper.deleteUserSessionsByIds(ids);
    }

    /**
     * 删除用户会话列表信息
     * 
     * @param id 用户会话列表主键
     * @return 结果
     */
    @Override
    public int deleteUserSessionsById(Long id)
    {
        return userSessionsMapper.deleteUserSessionsById(id);
    }
}
