package com.smartLive.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品对象 shop_goods (原 Voucher)
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@TableName("product")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺id */
    @Excel(name = "商铺id")
    private Long shopId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String name;

    /** 副标题 */
    @Excel(name = "副标题")
    private String subTitle;

    /** 核心字段 1：区分"它是什么？" 1:代金券, 2:团购套餐 */
    @Excel(name = "商品类别")
    private Integer category;

    /** 核心字段 2：区分"怎么卖？" 0:普通售卖, 1:秒杀活动 */
    @Excel(name = "活动类型")
    private Integer activityType;

    /** 核心字段 3：差异化数据存储 (JSON) */
    @Excel(name = "规则配置")
    private String rulesJson;

    /** 实际售价(秒杀价/团购价) */
    @Excel(name = "实际售价")
    private BigDecimal price;

    /** 原价 */
    @Excel(name = "原价")
    private BigDecimal originalPrice;

    /** 库存 */
    @Excel(name = "库存")
    private Integer stock;

    /** 销量 */
    private Integer sold;
    /** 商品状态 0:正常, 1:下架, 2:删除 */
    private Integer status;

    /** 评价数 */
    private Integer reviews;

    /** 粉丝数 */
    private Integer fans;

    /** 收藏数 */
    private Integer stars;

    /**
     * 有效期类型：1-固定日期，2-动态有效期（领券后N天有效）
     */
    private Integer validityType;

    /**
     * 固定日期的开始/结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useEndTime;

    /**
     * 动态有效期：领取后多少天有效
     */
    private Integer validDays;
    /**
     * 封面图片
     */
    private String coverImg;

    /**
     * 秒杀开始时间 (Merged from SeckillVoucher)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /**
     * 秒杀结束时间 (Merged from SeckillVoucher)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // --- Transient / Join Fields ---

    //店铺名称
    @TableField(exist = false)
    private String shopName;
    //店铺类型
    @TableField(exist = false)
    private Long typeId;
    //店铺logo
    @TableField(exist = false)
    private String shopLogo;
    //是否收藏
    @TableField(exist = false)
    private Boolean isStar;
    //是否关注
    @TableField(exist = false)
    private Boolean IsFollow;
    @TableField(exist = false)
    private double hotScore;}
