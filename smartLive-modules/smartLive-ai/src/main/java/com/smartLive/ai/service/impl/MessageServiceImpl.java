package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public List<Message> selectMessageList(Integer current,Long sessionId) {
        return query().eq("session_id",sessionId)
                        .orderByAsc("create_time")
                        .page(new Page<>(current, 10))
                        .getRecords();
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
        this.save(message);

        // Update session update_time
        Session session = new Session();
        session.setId(sessionId);
        session.setUpdateTime(new Date());
        sessionService.updateById(session);

        return message;
    }
}
