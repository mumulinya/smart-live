package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.InteractionRedisKeyConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 推送类型枚举
 */
@Getter
@AllArgsConstructor
public enum FeedTypeEnum {
    //全部推送
    ALL_FEED(0, InteractionRedisKeyConstants.Feed.ALL_FEED, "全部"),
    //用户推送
    USER_FEED(1, InteractionRedisKeyConstants.Feed.USER_FEED, "用户"),
    //店铺上新推送
    SHOP_FEED(2, InteractionRedisKeyConstants.Feed.SHOP_FEED, "店铺"),
    //商品推送
    ITEM_FEED(3, InteractionRedisKeyConstants.Feed.ITEM_FEED, "商品");
    private final Integer code;
    private final String feedKeyPrefix;
    private final String desc;

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
    // 生成最终 Redis Key 的工具方法
    public String getFullKey(Long userId) {
        return this.feedKeyPrefix + userId;
    }
    /**
     * 校验类型是否合法
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}