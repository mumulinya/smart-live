package com.smartLive.ai.service.merchant;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.AiMerchantMessage;

import java.util.List;

public interface IAiMerchantMessageService extends IService<AiMerchantMessage> {

    AiMerchantMessage saveMessage(Long sessionId, String role, String content,
                                  Long reviewId, Long productId, String timeRange);

    List<AiMerchantMessage> listBySessionId(Long sessionId);
}