package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * 评价生成数据传输对象。
 *
 * 作者：smartLive
 */
@Data
public class ReviewGenerateDTO {

    /** 店铺编号。 */
    private Long shopId;

    /** 订单编号。 */
    private Long orderId;

    /** 来源类型：2=店铺，3=博客，4=商品。 */
    private Integer sourceType;

    /** 来源编号。 */
    private Long sourceId;

    /** 总评分，范围 1-5。 */
    private Integer score;

    /** 服务评分。 */
    private Short serviceScore;

    /** 口味评分。 */
    private Short tasteScore;

    /** 环境评分。 */
    private Short envScore;

    /** 用户补充描述。 */
    private String description;
}
