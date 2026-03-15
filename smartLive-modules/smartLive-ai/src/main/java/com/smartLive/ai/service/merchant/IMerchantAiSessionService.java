package com.smartLive.ai.service.merchant;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.MerchantAiMessage;
import com.smartLive.ai.domain.MerchantAiSession;

import java.util.List;

public interface IMerchantAiSessionService extends IService<MerchantAiSession> {

    Long createSession(Long userId, Long shopId, String type);

    List<MerchantAiSession> listByUserAndShop(Long userId, Long shopId, String type);

    void updateTitle(Long userId, Long sessionId, String title);

    void deleteSession(Long userId, Long sessionId);

    List<MerchantAiMessage> getMessages(Long userId, Long sessionId);

    MerchantAiSession getAndCheckSession(Long userId, Long sessionId);

    void checkShopPermission(Long userId, Long shopId);
}