package com.smartlive.chat.service.impl;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import com.smartlive.chat.domain.ChatMessages;
import com.smartlive.chat.service.IChatMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
@Slf4j
public class UserSessionsServiceImpl  extends ServiceImpl<UserSessionsMapper, UserSessions> implements IUserSessionsService
{
    @Autowired
    private UserSessionsMapper userSessionsMapper;

    @Autowired
    private RemoteAppUserService remoteAppUserService;

    private final IChatMessagesService chatMessagesService;

    public UserSessionsServiceImpl(@Lazy IChatMessagesService chatMessagesService) {
        this.chatMessagesService = chatMessagesService;
    }

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
        log.info("userSessionsList:{}",userSessionsList.toString());
        userSessionsList.stream().forEach(c -> {
            //获取用户信息
            if(c!=null){
                Long id = c.getTargetUid();
                UserDTO user = remoteAppUserService.queryUserById(id);
                c.setNickname(user.getNickName());
                c.setAvatar(user.getIcon());
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
            }
        });
        // 按lastTime降序排序（最新的在前）
        List<UserSessions> sortedList = userSessionsList.stream()
                .filter(s -> s != null && s.getLastTime() != null)
                .sorted(Comparator.comparing(UserSessions::getLastTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        return sortedList;
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

    /**
     * 判断用户会话列表是否存在,不存在的话就创建会话列表
     *
     * @param userSessions
     * @return 结果
     */
    @Override
    public int isCreateUserSessions(UserSessions userSessions) {
        UserSessions one = query().eq("user_id", userSessions.getUserId())
                .eq("target_uid", userSessions.getTargetUid())
                .eq("session_id", userSessions.getSessionId()).one();
        if (one == null) {
            userSessions.setCreateTime(new Date());
            return insertUserSessions(userSessions);
        }
        return 0;
    }

    /**
     * 置顶会话列表
     *
     * @param
     * @return 结果
     */
    @Override
    public boolean isPin(UserSessions userSessions) {

        boolean update = this.lambdaUpdate()
                .eq(UserSessions::getId, userSessions.getId())
                .set(UserSessions::getPin, userSessions.getPin())
                .update();
        return update;
    }
}
