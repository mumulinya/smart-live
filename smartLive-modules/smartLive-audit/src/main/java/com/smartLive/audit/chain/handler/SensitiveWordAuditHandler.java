package com.smartLive.audit.chain.handler;

import com.smartLive.audit.chain.AuditContentExtractor;
import com.smartLive.audit.chain.AuditProcessContext;
import com.smartLive.audit.chain.AuditProcessHandler;
import com.smartLive.common.core.utils.SensitiveWordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 敏感词审核处理器
 */
@Slf4j
@Component
@Order(100)
public class SensitiveWordAuditHandler implements AuditProcessHandler {

    @Autowired
    private SensitiveWordUtil sensitiveWordUtil;
    @Autowired
    private AuditContentExtractor auditContentExtractor;

    @Override
    public void handle(AuditProcessContext context) {
        if (context == null || context.isFinished() || context.getAuditMessage() == null) {
            return;
        }

        String content = auditContentExtractor.extractText(context.getAuditMessage().getAuditContent());
        if (!sensitiveWordUtil.hasSensitiveWord(content)) {
            return;
        }

        List<String> sensitiveWords = sensitiveWordUtil.findAll(content);
        String reason = "你的消息包含敏感词：" + String.join(",", sensitiveWords);
        context.reject(reason);
        log.info("敏感词审核未通过，taskId={}, words={}", context.getAuditTaskId(), sensitiveWords);
    }
}

