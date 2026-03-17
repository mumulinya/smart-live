package com.smartLive.ai.service.merchant;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.MerchantAiMessage;

import java.util.List;

/**
 * 商家 AI 消息服务接口。
 */
public interface IMerchantAiMessageService extends IService<MerchantAiMessage> {

    /**
     * 保存消息。
     */
    MerchantAiMessage saveMessage(Long sessionId, String role, String content,
                                  Long reviewId, Long productId, String timeRange, Long analysisRecordId);

    /**
     * 按会话 ID 查询商家 AI 消息列表。
     */
    List<MerchantAiMessage> listBySessionId(Long sessionId);
}
