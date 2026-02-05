package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.Session;
import com.smartLive.ai.mapper.SessionMapper;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.common.security.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import com.smartLive.common.core.utils.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * AI Session Service Implementation
 *
 * @author smartLive
 */
@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements ISessionService {

    @Override
    public Long createSession(String title) {
        Session session = new Session();
        session.setUserId(SecurityUtils.getUserId());
        session.setTitle(StringUtils.isEmpty(title) ? "New Chat" : title);
        session.setCreateTime(new Date());
        session.setUpdateTime(new Date());
        session.setCreateBy(SecurityUtils.getUsername());
        this.save(session);
        return session.getId();
    }

    @Override
    public List<Session> selectSessionList(Session session) {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<Session> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Session::getUserId, userId);
        queryWrapper.orderByDesc(Session::getUpdateTime);
        return this.list(queryWrapper);
    }
}
