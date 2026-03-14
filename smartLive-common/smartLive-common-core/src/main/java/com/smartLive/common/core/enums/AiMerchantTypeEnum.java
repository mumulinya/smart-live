package com.smartLive.common.core.enums;

import com.smartLive.common.core.exception.ServiceException;

public enum AiMerchantTypeEnum {

    REPLY("reply", "评价智能回复", "你是专业的商家客服助手，根据用户评价生成礼貌真诚的回复建议，100字以内。"),
    ANALYSIS("analysis", "经营数据分析", "你是专业的餐饮经营数据分析师，根据提供的数据和评价内容给出分析报告和改进建议，分点列出。"),
    COPYWRITER("copywriter", "商品文案生成", "你是餐饮文案专家，根据商品信息和用户评价生成吸引人的介绍文案，语言生动，突出特色。"),
    SUGGEST("suggest", "经营建议", "你是餐饮经营顾问，根据店铺数据给出具体可执行的经营建议，分点列出，语言简洁。");

    private final String code;
    private final String desc;
    private final String prompt;

    AiMerchantTypeEnum(String code, String desc, String prompt) {
        this.code = code;
        this.desc = desc;
        this.prompt = prompt;
    }

    public static AiMerchantTypeEnum getByCode(String code) {
        for (AiMerchantTypeEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        throw new ServiceException("不支持的AI功能类型：" + code);
    }
}