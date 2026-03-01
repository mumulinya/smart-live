package com.smartLive.audit.chain;

import com.smartLive.common.rabbitmq.domain.AuditMessage;

/**
 * 审核责任链上下文
 * 用于在处理器之间传递任务信息与决策结果
 */
public class AuditProcessContext {

    /**
     * 审核任务ID（已落库）
     */
    private final Long auditTaskId;

    /**
     * 原始审核消息
     */
    private final AuditMessage auditMessage;

    /**
     * 当前链路决策，默认继续执行
     */
    private AuditDecision decision = AuditDecision.CONTINUE;

    /**
     * 审核说明（驳回原因/通过说明/转人工原因）
     */
    private String reason;

    public AuditProcessContext(Long auditTaskId, AuditMessage auditMessage) {
        this.auditTaskId = auditTaskId;
        this.auditMessage = auditMessage;
    }

    public Long getAuditTaskId() {
        return auditTaskId;
    }

    public AuditMessage getAuditMessage() {
        return auditMessage;
    }

    public AuditDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    /**
     * 标记为自动驳回并终止后续链路
     */
    public void reject(String reason) {
        this.decision = AuditDecision.REJECT;
        this.reason = reason;
    }

    /**
     * 标记为自动通过并终止后续链路
     */
    public void pass(String reason) {
        this.decision = AuditDecision.PASS;
        this.reason = reason;
    }

    /**
     * 标记为转人工审核并终止后续链路
     */
    public void waitManual(String reason) {
        this.decision = AuditDecision.WAIT_MANUAL;
        this.reason = reason;
    }

    /**
     * 当前链路是否已经产出终态决策
     */
    public boolean isFinished() {
        return decision != AuditDecision.CONTINUE;
    }
}

