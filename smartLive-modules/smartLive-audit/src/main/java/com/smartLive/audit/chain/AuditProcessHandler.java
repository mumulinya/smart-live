package com.smartLive.audit.chain;

/**
 * 审核责任链处理器接口
 */
public interface AuditProcessHandler {

    /**
     * 处理审核上下文
     *
     * @param context 审核上下文
     */
    void handle(AuditProcessContext context);
}

