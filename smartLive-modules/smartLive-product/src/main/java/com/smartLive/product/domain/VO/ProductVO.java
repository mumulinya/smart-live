package com.smartLive.product.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品返回对象 (View Object)
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@Data
public class ProductVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 商铺id */
    private String shopId;

    /** 商品名称 */
    private String name;

    /** 副标题 */
    private String subTitle;

    /** 商品类别 1:代金券, 2:团购套餐 */
    private Integer category;

    /** 活动类型 0:普通售卖, 1:秒杀活动 */
    private Integer activityType;

    /** 规则配置 (JSON) */
    private String rulesJson;

    /** 实际售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 销量 */
    private Integer sold;

    /** 评价数 */
    private Integer reviews;

    /** 粉丝数 */
    private Integer fans;

    /** 收藏数 */
    private Integer stars;
    private Integer status;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 有效期类型：1-固定日期，2-动态有效期 */
    private Integer validityType;

    /** 固定日期的开始/结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useEndTime;

    /** 动态有效期：领取后多少天有效 */
    private Integer validDays;

    /** 封面图片 */
    private String coverImg;

    /** 秒杀开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /** 秒杀结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    // --- Join Fields ---
    private Boolean isStar;
    private Boolean IsFollow;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}