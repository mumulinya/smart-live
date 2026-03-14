package com.smartLive.ai.service.merchant;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.AiMerchantMessage;
import com.smartLive.ai.domain.AiMerchantSession;

import java.util.List;

public interface IAiMerchantSessionService extends IService<AiMerchantSession> {

    Long createSession(Long userId, Long shopId, String type);

    List<AiMerchantSession> listByUserAndShop(Long userId, Long shopId, String type);

    void updateTitle(Long userId, Long sessionId, String title);

    void deleteSession(Long userId, Long sessionId);

    List<AiMerchantMessage> getMessages(Long userId, Long sessionId);

    AiMerchantSession getAndCheckSession(Long userId, Long sessionId);

    void checkShopPermission(Long userId, Long shopId);
}