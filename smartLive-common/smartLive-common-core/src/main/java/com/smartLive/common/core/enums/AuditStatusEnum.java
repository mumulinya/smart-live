package com.smartLive.common.core.enums;

import lombok.Getter;

/**
 * 审核状态枚举
 */
@Getter
public enum AuditStatusEnum {
    WAITING(0, "待审核"),
    PASS(1, "已发布/审核通过"),
    REJECT(2, "审核驳回"),
    DRAFT(3, "草稿"),
    OFFLINE(4, "下架/禁用");

    private final int code;
    private final String message;

    AuditStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}