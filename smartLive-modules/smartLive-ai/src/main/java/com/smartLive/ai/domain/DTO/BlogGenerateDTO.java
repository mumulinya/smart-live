package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * 博客生成数据传输对象。
 *
 * 作者：smartLive
 */
@Data
public class BlogGenerateDTO {

    /** 店铺编号。 */
    private Long shopId;

    /** 用户补充要求。 */
    private String description;

    /** 风格：0=自然，1=精致，2=幽默。 */
    private Integer style;
}
