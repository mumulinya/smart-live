package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.domain.Session;
import com.smartLive.ai.mapper.MessageMapper;
import com.smartLive.ai.service.IMessageService;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * AI Message Service Implementation
 *
 * @author smartLive
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    @Autowired
    private ISessionService sessionService;

    @Override
    public List<Message> selectMessageList(Long sessionId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getSessionId, sessionId);
        queryWrapper.orderByAsc(Message::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message saveMessage(Long sessionId, String role, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setType("text"); // Default type
        message.setCreateTime(new Date());
        message.setCreateBy(SecurityUtils.getUsername());
        this.save(message);

        // Update session update_time
        Session session = new Session();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        sessionService.updateById(session);

        return message;
    }
}
