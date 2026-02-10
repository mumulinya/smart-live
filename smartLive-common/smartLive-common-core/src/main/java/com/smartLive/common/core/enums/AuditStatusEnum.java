package com.smartLive.common.core.enums;
/**
 * 审核状态枚举
 */
public enum AuditStatusEnum {
    WAITING(0, "待审核"),
    PASS(1, "已发布/审核通过"),
    REJECT(2, "审核驳回"),
    DRAFT(3, "草稿"),
    OFFLINE(4, "下架/禁用");

    private final Integer code;
    private final String message;

    AuditStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}