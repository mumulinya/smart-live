package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.Session;
import com.smartLive.ai.mapper.SessionMapper;
import com.smartLive.ai.service.ISessionService;
import com.smartLive.common.core.constant.Constants;
import com.smartLive.common.core.context.UserContextHolder;
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
}
