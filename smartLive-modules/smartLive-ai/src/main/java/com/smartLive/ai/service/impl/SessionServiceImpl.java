package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.Session;
import com.smartLive.ai.mapper.SessionMapper;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.common.core.constant.Constants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.ai.domain.Message;
import com.smartLive.ai.service.IMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * AI Session Service Implementation
 *
 * @author smartLive
 */
@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements ISessionService {

    @Autowired
    @Lazy
    private IMessageService messageService;

    @Override
    public Long createSession(String title) {
        Session session = new Session();
        Long userId = UserContextHolder.getUser().getId();
        session.setUserId(userId);
        session.setTitle(StringUtils.isEmpty(title) ? "New Chat" : title);
        session.setCreateTime(new Date());
        session.setUpdateTime(new Date());
        this.save(session);
        return session.getId();
    }

    @Override
    public List<Session> selectSessionList(Integer current) {
        Long userId = UserContextHolder.getUser().getId();
        return query()
                .eq("user_id", userId)
                .orderByDesc("update_time")
                .page(new Page<>(current, 10))
                .getRecords();
    }
    @Override
    public List<Session> searchByKeyword(String keyword, Integer current) {
        // 获取当前登录用户ID
        Long userId = UserContextHolder.getUser().getId();
        Page<Session> page = new Page<>(current,10);
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getUserId, userId)
                .like(Session::getTitle, keyword)
                .orderByDesc(Session::getCreateTime);
        return this.page(page, wrapper).getRecords();
    }

    @Override
    public boolean deleteSession(Long sessionId) {
        Long userId = UserContextHolder.getUser().getId();
        Session session = this.getById(sessionId);

        if (session == null) {
            return false;
        }

        // Ensure user owns the session
        if (!session.getUserId().equals(userId)) {
            return false;
        }

        // Delete associated messages
        messageService.remove(new LambdaQueryWrapper<Message>().eq(Message::getSessionId, sessionId));

        // Delete session
        return this.removeById(sessionId);
    }

    @Override
    public boolean updateSessionTitle(Long sessionId, String title) {
        if (StringUtils.isBlank(title)) {
            return false;
        }
        
        Long userId = UserContextHolder.getUser().getId();
        Session session = this.getById(sessionId);
        
        if (session == null || !session.getUserId().equals(userId)) {
            return false;
        }
        
        session.setTitle(title);
        session.setUpdateTime(new Date());
        return this.updateById(session);
    }
}
