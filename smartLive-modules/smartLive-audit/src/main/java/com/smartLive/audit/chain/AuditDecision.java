package com.smartLive.audit.chain;

/**
 * 审核决策枚举
 */
public enum AuditDecision {
    /**
     * 继续执行后续处理器
     */
    CONTINUE,
    /**
     * 自动驳回
     */
    REJECT,
    /**
     * 自动通过
     */
    PASS,
    /**
     * 转人工审核
     */
    WAIT_MANUAL
}

