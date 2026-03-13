package com.smartLive.common.core.enums.common;

import lombok.Getter;

/**
 * 审核状态枚举
 */
@Getter
public enum AuditStatusEnum {
    /**
     * 待审核
     */
    WAITING(0, "待审核"),
    /**
     * 审核通过
     */
    PASS(1, "审核通过"),
    /**
     * 人工驳回（兼容历史值：2）
     */
    MANUAL_REJECT(2, "人工驳回"),
    /**
     * 自动驳回（敏感词/AI 等自动审核能力）
     */
    AUTO_REJECT(3, "自动驳回");

    private final int code;
    private final String message;

    AuditStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举，未命中返回 null
     */
    public static AuditStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AuditStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断是否为驳回态（自动驳回或人工驳回）
     *
     * @param code 状态码
     * @return true=驳回态
     */
    public static boolean isRejected(Integer code) {
        if (code == null) {
            return false;
        }
        return code == MANUAL_REJECT.code || code == AUTO_REJECT.code;
    }
}
