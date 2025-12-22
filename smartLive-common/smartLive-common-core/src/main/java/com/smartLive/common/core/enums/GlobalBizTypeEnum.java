package com.smartLive.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlobalBizTypeEnum {

    USER(1, "用户", "user"),
    SHOP(2, "店铺", "shop"),
    BLOG(3, "博客", "blog"),      // 注意：原来是1，现在改成了3
    VOUCHER(4, "代金券", "voucher"), // 注意：原来是3，现在改成了4
    COMMENT(5, "评论", "comment");   // 注意：原来是4，现在改成了5

    private final Integer code;
    private final String desc;
    private final String bizDomain; // 用于 MQ 或 Redis Key

    public static GlobalBizTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (GlobalBizTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}