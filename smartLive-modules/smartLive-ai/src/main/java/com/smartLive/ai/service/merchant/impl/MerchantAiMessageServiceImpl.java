package com.smartLive.ai.service.merchant.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.MerchantAiMessage;
import com.smartLive.ai.mapper.MerchantAiMessageMapper;
import com.smartLive.ai.service.merchant.IMerchantAiMessageService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 商家 AI 消息服务实现类。
 */
@Service
public class MerchantAiMessageServiceImpl extends ServiceImpl<MerchantAiMessageMapper, MerchantAiMessage>
        implements IMerchantAiMessageService {

    /**
     * 保存消息。
     */
    @Override
    public MerchantAiMessage saveMessage(Long sessionId, String role, String content,
                                         Long reviewId, Long productId, String timeRange, Long analysisRecordId) {
        MerchantAiMessage message = new MerchantAiMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setReviewId(reviewId);
        message.setProductId(productId);
        message.setTimeRange(timeRange);
        message.setAnalysisRecordId(analysisRecordId);
        message.setCreateTime(new Date());
        this.save(message);
        return message;
    }

    /**
     * 按会话 ID 查询商家 AI 消息列表。
     */
    @Override
    public List<MerchantAiMessage> listBySessionId(Long sessionId) {
        return baseMapper.selectBySessionId(sessionId);
    }
}
