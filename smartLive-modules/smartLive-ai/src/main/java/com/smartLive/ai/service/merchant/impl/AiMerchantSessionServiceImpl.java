package com.smartLive.ai.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.AiMerchantMessage;
import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.mapper.AiMerchantSessionMapper;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.system.api.RemoteUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class AiMerchantSessionServiceImpl extends ServiceImpl<AiMerchantSessionMapper, AiMerchantSession>
        implements IAiMerchantSessionService {

    private final IAiMerchantMessageService messageService;
    private final RemoteUserService remoteUserService;

    public AiMerchantSessionServiceImpl(IAiMerchantMessageService messageService, RemoteUserService remoteUserService) {
        this.messageService = messageService;
        this.remoteUserService = remoteUserService;
    }

    @Override
    public Long createSession(Long userId, Long shopId) {
        checkShopPermission(userId, shopId);
        Date now = new Date();
        AiMerchantSession session = new AiMerchantSession();
        session.setUserId(userId);
        session.setShopId(shopId);
        session.setTitle("new session");
        session.setCreateTime(now);
        session.setUpdateTime(now);
        this.save(session);
        return session.getId();
    }

    @Override
    public List<AiMerchantSession> listByUserAndShop(Long userId, Long shopId) {
        checkShopPermission(userId, shopId);
        LambdaQueryWrapper<AiMerchantSession> wrapper = new LambdaQueryWrapper<AiMerchantSession>()
                .eq(AiMerchantSession::getUserId, userId)
                .eq(AiMerchantSession::getShopId, shopId)
                .orderByDesc(AiMerchantSession::getUpdateTime)
                .orderByDesc(AiMerchantSession::getId);
        return this.list(wrapper);
    }

    @Override
    public void updateTitle(Long userId, Long sessionId, String title) {
        if (!StringUtils.hasText(title)) {
            throw new ServiceException("Title cannot be blank");
        }
        AiMerchantSession session = getAndCheckSession(userId, sessionId);
        session.setTitle(title.trim());
        session.setUpdateTime(new Date());
        this.updateById(session);
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        getAndCheckSession(userId, sessionId);
        messageService.remove(new LambdaQueryWrapper<AiMerchantMessage>().eq(AiMerchantMessage::getSessionId, sessionId));
        this.removeById(sessionId);
    }

    @Override
    public List<AiMerchantMessage> getMessages(Long userId, Long sessionId) {
        getAndCheckSession(userId, sessionId);
        return messageService.listBySessionId(sessionId);
    }

    @Override
    public AiMerchantSession getAndCheckSession(Long userId, Long sessionId) {
        AiMerchantSession session = this.getById(sessionId);
        if (session == null) {
            throw new ServiceException("Session not found");
        }
        checkShopPermission(userId, session.getShopId());
        if (session.getUserId() == null || !session.getUserId().equals(userId)) {
            throw new ServiceException("No access to this session");
        }
        return session;
    }

    @Override
    public void checkShopPermission(Long userId, Long shopId) {
        if (userId == null) {
            throw new ServiceException("User not logged in");
        }
        if (shopId == null) {
            throw new ServiceException("Shop id cannot be null");
        }
        if (SecurityUtils.isAdmin(userId)) {
            return;
        }
        List<Long> shopIds = remoteUserService.getShopIdsByUserId(userId);
        if (shopIds == null || !shopIds.contains(shopId)) {
            throw new ServiceException("No access to this shop");
        }
    }

}
