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
import java.util.List;

/**
 * 商品实体类
 * 对应数据库 product 表，统一管理代金券、团购套餐等商品信息
 *
 * @author smartLive
 * @date 2026-02-18
 */
@TableName("product")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product   implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺id (多个店铺用逗号分隔) */
    @Excel(name = "商铺id")
    private String shopId;

    /** 查询条件：店铺ID集合（非表字段） */
    @TableField(exist = false)
    private List<Long> shopIds;

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
    /** 
     * 商品状态 
     * PENDING(0): 待审核
     * NORMAL(1): 正常/上架
     * OFF_SHELF(2): 下架
     * AUDIT_FAIL(3): 审核失败
     * EXPIRED(4): 过期
     */
    private Integer status;

    /** 审核拒绝原因 */
    private String rejectReason;

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

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

}
