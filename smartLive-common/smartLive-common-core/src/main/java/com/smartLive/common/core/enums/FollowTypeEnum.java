package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.FollowTypeConstants;
import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FollowTypeEnum {
    // 定义类型，同时绑定对应的 Redis Key 前缀
    USERINFO(FollowTypeConstants.USER, "userInfo","用户关注",RedisConstants.FOLLOW_USER_KEY, RedisConstants.FANS_USER_KEY),
    SHOPINFO(FollowTypeConstants.SHOP, "shopInfo","店铺关注",RedisConstants.FOLLOW_SHOP_KEY, RedisConstants.FANS_SHOP_KEY);
    private final Integer code;
    private final String key;
    private final String desc;
    private final String followKeyPrefix; // "follow:user:", "follow:shop:"
    private final String fansKeyPrefix;   // "fans:user:", "fans:shop:"
    
    // 简单的根据 code 获取枚举的方法
    public static FollowTypeEnum getByCode(Integer code) {
        for (FollowTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}