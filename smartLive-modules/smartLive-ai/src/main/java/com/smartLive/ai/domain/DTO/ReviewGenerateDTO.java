package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * 评价生成数据传输对象。
 *
 * 作者：smartLive
 */
@Data
public class ReviewGenerateDTO {

    /** 店铺 ID。 */
    private Long shopId;

    /** 订单 ID。 */
    private Long orderId;

    /** 来源类型：2=店铺，3=博客，4=商品。 */
    private Integer sourceType;

    /** Source id. */
    private Long sourceId;

    /** 总评分，范围 1-5。 */
    private Integer score;

    /** Service score. */
    private Short serviceScore;

    /** Taste score. */
    private Short tasteScore;

    /** Environment score. */
    private Short envScore;

    /** 用户补充描述。 */
    private String description;
}
