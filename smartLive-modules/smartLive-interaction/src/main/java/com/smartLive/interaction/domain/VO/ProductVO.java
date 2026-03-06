package com.smartLive.interaction.domain.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description: 商品VO
 * @Author:  mumulin
 * @Date:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVO {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺id */
    @Excel(name = "商铺id")
    private String shopId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String name;

    /** 副标题 */
    @Excel(name = "副标题")
    private String subTitle;

    /** 商品类别 1:代金券, 2:团购套餐 */
    @Excel(name = "商品类别")
    private Integer category;

    /** 活动类型 0:普通售卖, 1:秒杀活动 */
    @Excel(name = "活动类型")
    private Integer activityType;

    /** 规则配置 (JSON) */
    @Excel(name = "规则配置")
    private String rulesJson;

    /** 实际售价 */
    @Excel(name = "实际售价")
    private BigDecimal price;

    /** 原价 */
    @Excel(name = "原价")
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 状态 */
    @Excel(name = "状态")
    private Integer status;

    /** 有效期类型 */
    private Integer validityType;

    /** 固定日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useEndTime;

    /** 动态有效期 */
    private Integer validDays;

    /** 封面图片 */
    private String coverImg;

    /** 秒杀开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /** 秒杀结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    // Join Fields
    private String shopName;
    private String shopLogo; // ProductDTO has shopImages, ProductVO had shopLogo/shopImages.
    private Long typeId;
    private Double hotSource;
}
