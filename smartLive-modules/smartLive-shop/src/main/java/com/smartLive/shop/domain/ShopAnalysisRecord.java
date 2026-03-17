package com.smartLive.shop.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 店铺经营分析记录实体。
 */
@Data
@TableName("shop_analysis_record")
public class ShopAnalysisRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 店铺ID */
    private Long shopId;

    /** 时间范围标识 */
    private String timeRange;

    /** 统计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 统计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 订单总数 */
    private Integer totalOrders;

    /** 订单总收入 */
    private BigDecimal totalRevenue;

    /** 平均评分 */
    private BigDecimal avgScore;

    /** 差评数量 */
    private Integer badReviewCount;

    /** 热销商品列表（JSON） */
    private String hotProducts;

    /** 滞销商品列表（JSON） */
    private String slowProducts;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
