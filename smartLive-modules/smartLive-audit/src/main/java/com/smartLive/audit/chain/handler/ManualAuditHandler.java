package com.smartLive.audit.chain.handler;

import com.smartLive.audit.chain.AuditProcessContext;
import com.smartLive.audit.chain.AuditProcessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 人工审核路由处理器
 * 当自动审核未产出终态决策时，统一转人工审核
 */
@Slf4j
@Component
@Order(300)
public class ManualAuditHandler implements AuditProcessHandler {

    @Override
    public void handle(AuditProcessContext context) {
        if (context == null || context.isFinished()) {
            return;
        }
        context.waitManual("自动审核未命中终态，已转人工审核");
        log.info("任务已转人工审核，taskId={}", context.getAuditTaskId());
    }
}

