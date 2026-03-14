package com.smartLive.ai.service.merchant.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.AiMerchantMessage;
import com.smartLive.ai.mapper.AiMerchantMessageMapper;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AiMerchantMessageServiceImpl extends ServiceImpl<AiMerchantMessageMapper, AiMerchantMessage>
        implements IAiMerchantMessageService {

    @Override
    public AiMerchantMessage saveMessage(Long sessionId, String role, String content,
                                         Long reviewId, Long productId, String timeRange) {
        AiMerchantMessage message = new AiMerchantMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setReviewId(reviewId);
        message.setProductId(productId);
        message.setTimeRange(timeRange);
        message.setCreateTime(new Date());
        this.save(message);
        return message;
    }

    @Override
    public List<AiMerchantMessage> listBySessionId(Long sessionId) {
        return baseMapper.selectBySessionId(sessionId);
    }
}