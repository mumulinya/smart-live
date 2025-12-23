package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资源类型总枚举
 * 职责：
 * 1. 定义系统中所有的资源类型 (Code, 描述)
 * 2. 提供策略模式所需的 Bean 名称 (strategyName)
 * 3. 提供 MQ/API 用的业务域标识 (bizDomain)
 * 4. (暂时保留) 评论和收藏的配置，未来建议也像点赞一样抽离出去
 */
@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {

    // 格式：Code, 策略Bean名, 描述, 业务域, 评论Key, 收藏Key

    // 1. 博客
    BLOG_RESOURCE(GlobalBizTypeEnum.BLOG.getCode(), "博客资源", GlobalBizTypeEnum.BLOG.getBizDomain(),
            RedisConstants.BLOG_COMMENT_KEY, RedisConstants.BLOG_STAR_KEY),

    // 2. 店铺
    SHOP_RESOURCE(GlobalBizTypeEnum.SHOP.getCode(), "店铺资源", GlobalBizTypeEnum.SHOP.getBizDomain(),
            RedisConstants.SHOP_COMMENT_KEY, null),

    // 3. 代金券
    VOUCHER_RESOURCE(GlobalBizTypeEnum.VOUCHER.getCode(), "代金券资源", GlobalBizTypeEnum.VOUCHER.getBizDomain(),
            RedisConstants.VOUCHER_COMMENT_KEY, RedisConstants.VOUCHER_STAR_KEY),

    // 4. 评论 (支持楼中楼)
    COMMENT_RESOURCE(GlobalBizTypeEnum.COMMENT.getCode(), "评论资源", GlobalBizTypeEnum.COMMENT.getBizDomain(),
            RedisConstants.COMMENT_COMMENT_KEY, RedisConstants.COMMENT_STAR_KEY);


    /**
     * 业务类型编码 (数据库存储值)
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

    // ========================================================================
    // 下面这两个字段建议未来也参考 LikeResourceTypeEnum 进行抽离
    // 目前保留是为了不报错
    // ========================================================================

    /**
     * 评论 Redis Key 前缀
     */
    private final String commentKeyPrefix;

    /**
     * 收藏 Redis Key 前缀
     */
    private final String collectKeyPrefix;


    /**
     * 根据 code 获取枚举
     */
    public static ResourceTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (ResourceTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}