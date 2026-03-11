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

    /**
     * 创建全新会话
     *
     * @param title 会话标题，若为空则默认设为 "New Chat"
     * @return 产生的会话 ID
     */
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

    /**
     * 查询用户会话列表（分页）
     *
     * @param current 当前页码
     * @return 会话分页数据
     */
    @Override
    public List<Session> selectSessionList(Integer current) {
        Long userId = UserContextHolder.getUser().getId();
        return query()
                .eq("user_id", userId)
                .orderByDesc("update_time")
                .page(new Page<>(current, 10))
                .getRecords();
    }
    /**
     * 根据关键词搜索会话
     *
     * @param keyword 标题关键词
     * @param current 当前页码
     * @return 搜索结果列表
     */
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

    /**
     * 删除会话及其关联的所有消息记录
     *
     * @param sessionId 会话 ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(Long sessionId) {
        Long userId = UserContextHolder.getUser().getId();
        Session session = this.getById(sessionId);

        if (session == null) {
            return false;
        }

        // 越权检查
        if (!session.getUserId().equals(userId)) {
            return false;
        }

        // 联动删除消息表数据
        messageService.remove(new LambdaQueryWrapper<Message>().eq(Message::getSessionId, sessionId));

        // 删除会话本身
        return this.removeById(sessionId);
    }

    /**
     * 重命名会话
     */
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
