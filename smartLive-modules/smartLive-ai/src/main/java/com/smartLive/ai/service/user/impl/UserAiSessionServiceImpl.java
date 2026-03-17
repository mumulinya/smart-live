package com.smartLive.ai.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.UserAiMessage;
import com.smartLive.ai.domain.UserAiSession;
import com.smartLive.ai.mapper.UserAiSessionMapper;
import com.smartLive.ai.service.user.IUserAiMessageService;
import com.smartLive.ai.service.user.IUserAiSessionService;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户 AI 会话服务实现类。
 *
 * 作者：smartLive
 */
@Service
public class UserAiSessionServiceImpl extends ServiceImpl<UserAiSessionMapper, UserAiSession> implements IUserAiSessionService {

    @Autowired
    @Lazy
    private IUserAiMessageService messageService;

    /**
     * 创建全新会话
     *
     * @param 标题 会话标题，若为空则默认设为“新会话”
     * @return 产生的会话编号
     */
    @Override
    public Long createSession(String title) {
        UserAiSession session = new UserAiSession();
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
    public List<UserAiSession> selectSessionList(Integer current) {
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
    public List<UserAiSession> searchByKeyword(String keyword, Integer current) {
        // 获取当前登录用户编号
        Long userId = UserContextHolder.getUser().getId();
        Page<UserAiSession> page = new Page<>(current,10);
        LambdaQueryWrapper<UserAiSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAiSession::getUserId, userId)
                .like(UserAiSession::getTitle, keyword)
                .orderByDesc(UserAiSession::getCreateTime);
        return this.page(page, wrapper).getRecords();
    }

    /**
     * 删除会话及其关联的所有消息记录
     *
     * @param sessionId 会话编号
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(Long sessionId) {
        Long userId = UserContextHolder.getUser().getId();
        UserAiSession session = this.getById(sessionId);

        if (session == null) {
            return false;
        }

        // 越权检查
        if (!session.getUserId().equals(userId)) {
            return false;
        }

        // 联动删除消息表数据
        messageService.remove(new LambdaQueryWrapper<UserAiMessage>().eq(UserAiMessage::getSessionId, sessionId));

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
        UserAiSession session = this.getById(sessionId);
        
        if (session == null || !session.getUserId().equals(userId)) {
            return false;
        }
        
        session.setTitle(title);
        session.setUpdateTime(new Date());
        return this.updateById(session);
    }
}
