package com.smartLive.audit.chain.handler;

import com.smartLive.audit.chain.AuditProcessContext;
import com.smartLive.audit.chain.AuditProcessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AI 审核处理器（占位实现）
 * 当前仅输出日志，后续可在此接入模型审核服务
 */
@Slf4j
@Component
@Order(200)
public class AiAuditHandler implements AuditProcessHandler {

    @Override
    public void handle(AuditProcessContext context) {
        if (context == null || context.isFinished() || context.getAuditMessage() == null) {
            return;
        }
        log.info("AI审核占位处理器执行，taskId={}, bizType={}",
                context.getAuditTaskId(),
                context.getAuditMessage().getBizType());
    }
}

