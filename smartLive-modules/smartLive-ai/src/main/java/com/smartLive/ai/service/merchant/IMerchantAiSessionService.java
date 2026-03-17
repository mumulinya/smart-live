package com.smartLive.ai.service.merchant;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.MerchantAiMessage;
import com.smartLive.ai.domain.MerchantAiSession;

import java.util.List;

/**
 * 商家 AI 会话服务接口。
 */
public interface IMerchantAiSessionService extends IService<MerchantAiSession> {

    /**
     * 创建会话。
     */
    Long createSession(Long userId, Long shopId, String type);

    /**
     * 按用户和店铺查询商家 AI 会话列表。
     */
    List<MerchantAiSession> listByUserAndShop(Long userId, Long shopId, String type);

    /**
     * 更新标题。
     */
    void updateTitle(Long userId, Long sessionId, String title);

    /**
     * 删除会话。
     */
    void deleteSession(Long userId, Long sessionId);

    /**
     * 查询消息列表。
     */
    List<MerchantAiMessage> getMessages(Long userId, Long sessionId);

    /**
     * 获取并校验会话。
     */
    MerchantAiSession getAndCheckSession(Long userId, Long sessionId);

    /**
     * 校验店铺权限。
     */
    void checkShopPermission(Long userId, Long shopId);
}