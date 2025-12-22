package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IdentityTypeEnum {
    //用户信息
    USER_IDENTITY(GlobalBizTypeEnum.USER.getCode(), "user","用户信息",RedisConstants.FOLLOW_USER_KEY, RedisConstants.FANS_USER_KEY),
    //店铺信息
    SHOP_IDENTITY(GlobalBizTypeEnum.SHOP.getCode(), "shop","店铺信息",RedisConstants.FOLLOW_SHOP_KEY, RedisConstants.FANS_SHOP_KEY);
    private final Integer code;
    /**
     * 业务域标识
     */
    private final String bizDomain;
    /**
     * 描述
     */
    private final String desc;
    /**
     * 关注的 Redis Key 前缀
     */
    private final String followKeyPrefix; // "follow:user:", "follow:shop:"
    /**
     * 粉丝的 Redis Key 前缀
     */
    private final String fansKeyPrefix;   // "fans:user:", "fans:shop:"
    // 简单的根据 code 获取枚举的方法
    public static IdentityTypeEnum getByCode(Integer code) {
        for (IdentityTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}