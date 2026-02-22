package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.ResourceTypeConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
/**
 * 全局业务类型枚举
 * 1. 用于数据库存储业务类型
 * 2. 用于 MQ 消息的标识
 * 3. 用于 Redis Key 的标识
 */
@Getter
@AllArgsConstructor
public enum GlobalBizTypeEnum {

    USER(ResourceTypeConstants.USER_CODE, "用户", "user"),
    SHOP(ResourceTypeConstants.SHOP_CODE, "店铺", "shop"),
    BLOG(ResourceTypeConstants.BLOG_CODE, "博客", "blog"),      // 注意：原来是1，现在改成了3
    PRODUCT(ResourceTypeConstants.PRODUCT_CODE, "商品", "product"), // 注意：原来是3，现在改成了4
    COMMENT(ResourceTypeConstants.COMMENT_CODE, "评论", "comment"),   // 注意：原来是4，现在改成了5
    REVIEW(ResourceTypeConstants.REVIEW_CODE,"评价","review");

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
    public static GlobalBizTypeEnum getByBizDomain(String bizDomain){
        if (bizDomain == null) return null;
        for (GlobalBizTypeEnum e : values()) {
            if (e.bizDomain.equals(bizDomain)) return e;
        }
        return null;
    }
}