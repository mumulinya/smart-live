package com.smartLive.ai.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.MerchantAiMessage;
import com.smartLive.ai.domain.MerchantAiSession;
import com.smartLive.ai.mapper.MerchantAiSessionMapper;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import com.smartLive.ai.service.merchant.IMerchantAiSessionService;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.system.api.RemoteUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 商家 AI 会话服务实现类。
 */
@Service
public class MerchantAiSessionServiceImpl extends ServiceImpl<MerchantAiSessionMapper, MerchantAiSession>
        implements IMerchantAiSessionService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("reply", "analysis", "copywrite", "suggest");

    private final IMerchantAiMessageService messageService;
    private final RemoteUserService remoteUserService;

    /**
     * 构造商家 AI 会话服务实现类。
     */
    public MerchantAiSessionServiceImpl(IMerchantAiMessageService messageService, RemoteUserService remoteUserService) {
        this.messageService = messageService;
        this.remoteUserService = remoteUserService;
    }

    /**
     * 创建会话。
     */
    @Override
    public Long createSession(Long userId, Long shopId, String type) {
        checkShopPermission(userId, shopId);
        String normalizedType = normalizeAndCheckType(type);
        Date now = new Date();
        MerchantAiSession session = new MerchantAiSession();
        session.setUserId(userId);
        session.setShopId(shopId);
        session.setType(normalizedType);
        session.setTitle("new session");
        session.setCreateTime(now);
        session.setUpdateTime(now);
        this.save(session);
        return session.getId();
    }

    /**
     * 按用户和店铺查询商家 AI 会话列表。
     */
    @Override
    public List<MerchantAiSession> listByUserAndShop(Long userId, Long shopId, String type) {
        checkShopPermission(userId, shopId);
        String normalizedType = normalizeAndCheckType(type);
        LambdaQueryWrapper<MerchantAiSession> wrapper = new LambdaQueryWrapper<MerchantAiSession>()
                .eq(MerchantAiSession::getUserId, userId)
                .eq(MerchantAiSession::getShopId, shopId)
                .eq(MerchantAiSession::getType, normalizedType)
                .orderByDesc(MerchantAiSession::getUpdateTime)
                .orderByDesc(MerchantAiSession::getId);
        return this.list(wrapper);
    }

    /**
     * 更新标题。
     */
    @Override
    public void updateTitle(Long userId, Long sessionId, String title) {
        if (!StringUtils.hasText(title)) {
            throw new ServiceException("Title cannot be blank");
        }
        MerchantAiSession session = getAndCheckSession(userId, sessionId);
        session.setTitle(title.trim());
        session.setUpdateTime(new Date());
        this.updateById(session);
    }

    /**
     * 删除会话。
     */
    @Override
    public void deleteSession(Long userId, Long sessionId) {
        getAndCheckSession(userId, sessionId);
        messageService.remove(new LambdaQueryWrapper<MerchantAiMessage>().eq(MerchantAiMessage::getSessionId, sessionId));
        this.removeById(sessionId);
    }

    /**
     * 查询消息列表。
     */
    @Override
    public List<MerchantAiMessage> getMessages(Long userId, Long sessionId) {
        getAndCheckSession(userId, sessionId);
        return messageService.listBySessionId(sessionId);
    }

    /**
     * 获取并校验会话。
     */
    @Override
    public MerchantAiSession getAndCheckSession(Long userId, Long sessionId) {
        MerchantAiSession session = this.getById(sessionId);
        if (session == null) {
            throw new ServiceException("Session not found");
        }
        checkShopPermission(userId, session.getShopId());
        if (session.getUserId() == null || !session.getUserId().equals(userId)) {
            throw new ServiceException("No access to this session");
        }
        return session;
    }

    /**
     * 校验店铺权限。
     */
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

    /**
     * 规范化check类型。
     */
    private String normalizeAndCheckType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new ServiceException("Session type cannot be blank");
        }
        String normalizedType = type.trim().toLowerCase();
        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            throw new ServiceException("Unsupported session type: " + type);
        }
        return normalizedType;
    }
}