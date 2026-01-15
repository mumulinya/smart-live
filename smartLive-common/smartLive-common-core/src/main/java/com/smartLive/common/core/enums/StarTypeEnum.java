package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收藏类型总枚举
 * 职责：
 * 1. 收藏类型编码 (数据库存储值)
 * 2. 提供策略模式所需的 Bean 名称 (strategyName)
 * 3. 提供 MQ/API 用的业务域标识 (bizDomain)
 */
@Getter
@AllArgsConstructor
public enum StarTypeEnum {

    // 1. 博客收藏配置
    BLOG_STAR(GlobalBizTypeEnum.BLOG.getCode(), "博客的收藏",GlobalBizTypeEnum.BLOG.getBizDomain(),
            RedisConstants.BLOG_STAR_KEY,
            RedisConstants.BLOG_STAR_COUNT_KEY,
            RedisConstants.BLOG_STAR_DIRTY_KEY),
    // 2. 店铺收藏配置
    SHOP_STAR(GlobalBizTypeEnum.SHOP.getCode(), "店铺的收藏",GlobalBizTypeEnum.SHOP.getBizDomain(),
            RedisConstants.SHOP_STAR_KEY,
            RedisConstants.SHOP_STAR_COUNT_KEY,
            RedisConstants.SHOP_STAR_DIRTY_KEY);

    /**
     * 业务类型编码 (与 ResourceTypeEnum 保持一致)
     */
    private final Integer code;
    
    /**
     * 描述
     */
    private final String desc;
    
    /**
     * 业务域标识
     */
    private final String bizDomain;

    /**
     * 1. 用户点赞关系 Key (Set结构: 存userId)
     * e.g. likes:blog:101 -> {user1, user2}
     */
    private final String starKeyPrefix;

    /**
     * 2. 点赞计数 Key (String结构: 存数字)
     * e.g. likes:count:blog:101 -> 99
     */
    private final String starCountKeyPrefix;

    /**
     * 3. 脏数据 Key (Set结构: 存bizId，用于定时任务同步)
     * e.g. likes:dirty:blog -> {101, 102}
     */
    private final String starDirtyKeyPrefix;

    /**
     * 根据 code 获取枚举
     * 如果传入不支持点赞的 code (如店铺 2)，这里直接返回 null
     */
    public static StarTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (StarTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}