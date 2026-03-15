package com.smartLive.ai.service.merchant;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.MerchantAiMessage;

import java.util.List;

public interface IMerchantAiMessageService extends IService<MerchantAiMessage> {

    MerchantAiMessage saveMessage(Long sessionId, String role, String content,
                                  Long reviewId, Long productId, String timeRange);

    List<MerchantAiMessage> listBySessionId(Long sessionId);
}