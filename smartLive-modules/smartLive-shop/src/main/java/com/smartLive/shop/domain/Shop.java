package com.smartLive.shop.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import java.io.Serializable;
import java.util.Date;

/**
 * 店铺对象 tb_shop
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@TableName("shop")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Shop extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺名称 */
    @Excel(name = "商铺名称")
    private String name;

    /** 商铺类型的id */
    @Excel(name = "商铺类型的id")
    private Long typeId;

    /** 商铺图片，多个图片以','隔开 */
    @Excel(name = "商铺图片，多个图片以','隔开")
    private String images;

    /** 店铺头像 */
    @Excel(name = "店铺头像")
    private String shopLogo;

    /** 商圈，例如陆家嘴 */
    @Excel(name = "商圈，例如陆家嘴")
    private String area;

    /** 地址 */
    @Excel(name = "地址")
    private String address;

    /** 经度 */
    @Excel(name = "经度")
    private Double x;

    /** 维度 */
    @Excel(name = "维度")
    private Double y;

    /** 均价，取整数 */
    @Excel(name = "均价，取整数")
    private Integer avgPrice;

    /** 销量 */
    @Excel(name = "销量")
    private Integer sold;

    /** 评论数量 */
    @Excel(name = "评论数量")
    private Integer reviews;
    /** 收藏数量 */
    @Excel(name = "收藏数量")
    private Integer stared;
    /** fans count */
    @Excel(name = "fans")
    private Integer fans;

    /** 评分，1~5分，乘10保存，避免小数 */
    @Excel(name = "评分，1~5分，乘10保存，避免小数")
    private Integer score;

    /** 营业时间，例如 10:00-22:00 */
    @Excel(name = "营业时间，例如 10:00-22:00")
    private String openHours;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}
