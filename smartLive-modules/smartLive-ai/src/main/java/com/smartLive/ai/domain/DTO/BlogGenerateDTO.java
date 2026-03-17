package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * 博客生成数据传输对象。
 *
 * 作者：smartLive
 */
@Data
public class BlogGenerateDTO {

    /** 店铺 ID。 */
    private Long shopId;

    /** 用户补充要求。 */
    private String description;

    /** Style: 0=natural, 1=refined, 2=humorous. */
    private Integer style;
}
