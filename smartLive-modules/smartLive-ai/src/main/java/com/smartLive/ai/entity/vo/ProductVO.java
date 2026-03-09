package com.smartLive.ai.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 商品对象
 * 
 * @author ruoyi
 * @date 2026-02-18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVO implements Serializable
{

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺id */
    private String shopId;

    /** 商品名称 */
    private String name;

    /** 副标题 */
    private String subTitle;

    /** 规则配置 */
    private String rulesJson;

    /** 价格 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 活动类型 0:普通售卖, 1:秒杀活动 */
    private Integer activityType;

    /** 商品类型 1:代金券, 2:团购套餐 */
    private Integer category;

    /** 封面图片 */
    private String coverImg;

    /** 1,上架; 2,下架; 3,过期 */
    private Integer status;

    private Long userId; // For order context

    /**
     * 库存
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 秒杀开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /**
     * 秒杀结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 有效期类型：1-固定日期，2-动态有效期 */
    private Integer validityType;
    /** 固定日期的开始/结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useEndTime;

    /** 动态有效期：领取后多少天有效 */
    private Integer validDays;
}
