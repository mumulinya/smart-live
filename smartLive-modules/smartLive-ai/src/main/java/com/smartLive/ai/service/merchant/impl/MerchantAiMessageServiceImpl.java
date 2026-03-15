package com.smartLive.ai.service.merchant.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.MerchantAiMessage;
import com.smartLive.ai.mapper.MerchantAiMessageMapper;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MerchantAiMessageServiceImpl extends ServiceImpl<MerchantAiMessageMapper, MerchantAiMessage>
        implements IMerchantAiMessageService {

    @Override
    public MerchantAiMessage saveMessage(Long sessionId, String role, String content,
                                         Long reviewId, Long productId, String timeRange) {
        MerchantAiMessage message = new MerchantAiMessage();
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
    public List<MerchantAiMessage> listBySessionId(Long sessionId) {
        return baseMapper.selectBySessionId(sessionId);
    }
}