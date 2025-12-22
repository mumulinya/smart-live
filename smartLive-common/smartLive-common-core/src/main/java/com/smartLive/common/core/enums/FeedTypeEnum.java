package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 业务类型枚举
 * 用于：收藏表(sys_favorite)、点赞表、Feed流推送内容类型
 */
@Getter
@AllArgsConstructor
public enum FeedTypeEnum {
    //博客推送
    BLOG_FEED(GlobalBizTypeEnum.BLOG.getCode(), RedisConstants.BLOG_FEED_KEY, "博客", GlobalBizTypeEnum.BLOG.getBizDomain()),
    //代金券推送
    VOUCHER_FEED(GlobalBizTypeEnum.VOUCHER.getCode(), RedisConstants.VOUCHER_FEED_KEY, "代金券", GlobalBizTypeEnum.VOUCHER.getBizDomain()),
    //秒杀代金券推送
    SECKILL_VOUCHER_FEED(GlobalBizTypeEnum.VOUCHER.getCode(), RedisConstants.SECKILL_VOUCHER_FEED_KEY, "秒杀代金券", GlobalBizTypeEnum.VOUCHER.getBizDomain());
    private final Integer code;
    private final String feedKeyPrefix;
    private final String desc;
    private final String bizDomain; // 用于 MQ 或 Redis Key

    /**
     * 根据 code 获取枚举对象 (用于数据库值转枚举)
     */
    public static FeedTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        // 使用 Stream 流查找，或者用增强 for 循环
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null); // 或者抛出异常，看业务需求
    }

    /**
     * 校验类型是否合法
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}