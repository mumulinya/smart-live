package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评价类型枚举
 */
@Getter
@AllArgsConstructor
public enum ReviewTypeEnum {

    SHOP_REVIEW(
            GlobalBizTypeEnum.SHOP.getCode(),
            "店铺评价",
            GlobalBizTypeEnum.SHOP.getBizDomain(),
            RedisConstants.SHOP_REVIEW_HOT_RANK_KEY,
            RedisConstants.SHOP_REVIEW_COUNT_KEY,
            RedisConstants.USER_REVIEW_KEY,
            RedisConstants.SHOP_REVIEW_NEW_RANK_KEY,
            RedisConstants.SHOP_REVIEW_SYNC_KEY,
            RedisConstants.SHOP_REVIEW_CALC_KEY
    ),

    PRODUCT_REVIEW(
            GlobalBizTypeEnum.PRODUCT.getCode(),
            "商品评价",
            GlobalBizTypeEnum.PRODUCT.getBizDomain(),
            RedisConstants.PRODUCT_REVIEW_HOT_RANK_KEY,
            RedisConstants.PRODUCT_REVIEW_COUNT_KEY,
            RedisConstants.USER_REVIEW_KEY,
            RedisConstants.PRODUCT_REVIEW_NEW_RANK_KEY,
            RedisConstants.PRODUCT_REVIEW_SYNC_KEY,
            RedisConstants.PRODUCT_REVIEW_CALC_KEY
    );

    private final Integer code;
    private final String desc;
    private final String bizDomain;

    /**
     * 热门榜前缀（ZSet）
     */
    private final String reviewHotRankKeyPrefix;

    /**
     * 互动计数前缀（String）
     */
    private final String reviewCountKeyPrefix;

    /**
     * 用户评价反向索引前缀（ZSet）
     */
    private final String userReviewKeyPrefix;

    /**
     * 最新榜前缀（ZSet）
     */
    private final String reviewNewRankKeyPrefix;

    /**
     * 落库队列 key（Set）
     */
    private final String reviewSyncKey;

    /**
     * 算分队列 key（Set）
     */
    private final String reviewCalcKey;

    public static ReviewTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
