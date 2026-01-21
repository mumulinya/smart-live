package com.smartLive.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品动作枚举
 * @author smartLive
 */
@Getter
@AllArgsConstructor
public enum ItemActionType {

    /** 价格下降 */
    PRICE_DROP("price_drop", "降价提醒"),
    
    /** 补货 */
    RESTOCK("restock", "补货提醒"),
    
    /** 开抢 */
    START("start", "开抢提醒"),
    
    /** 即将下架 */
    SOON_END("soon_end", "即将下架"),
    
    /** 重新上架 */
    RESHELF("reshelf", "重新上架"),
    
    /** 新品 */
    NEW_ITEM("new", "新品推荐");

    private final String code;
    private final String desc;
    
    /**
     * 根据 code 获取枚举
     */
    public static ItemActionType getByCode(String code) {
        for (ItemActionType value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}