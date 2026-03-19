package com.smartLive.audit.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审核责任链执行器
 * Spring 会按 @Order 顺序注入处理器列表
 */
@Slf4j
@Component
public class AuditProcessChain {

    private final List<AuditProcessHandler> handlers;

    /**
     * 构建审核责任链
     *
     * @param handlers 处理器列表（按 @Order 排序）
     */
    @Autowired
    public AuditProcessChain(List<AuditProcessHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 执行责任链，直到某个处理器给出终态决策
     *
     * @param context 审核上下文
     */
    public void execute(AuditProcessContext context) {
        if (context == null) {
            return;
        }
        for (AuditProcessHandler handler : handlers) {
            handler.handle(context);
            if (context.isFinished()) {
                log.info("审核责任链已终止，taskId={}, decision={}", context.getAuditTaskId(), context.getDecision());
                return;
            }
        }
    }
}
