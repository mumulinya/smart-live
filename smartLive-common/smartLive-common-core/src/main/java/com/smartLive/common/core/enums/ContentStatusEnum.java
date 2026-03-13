package com.smartLive.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Content business status enum for blog/review.
 */
@Getter
@AllArgsConstructor
public enum ContentStatusEnum {

    /** Draft */
    DRAFT(0, "DRAFT"),
    /** Published */
    PUBLISHED(1, "PUBLISHED"),
    /** Off shelf */
    OFF(2, "OFF");

    private final Integer code;
    private final String desc;

    public static ContentStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ContentStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
