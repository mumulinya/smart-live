package com.smartLive.shop.api.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 店铺对象 tb_shop
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopDTO extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺名称 */
    private String name;

    /** 商铺类型的id */
    private Long typeId;

    /** 商铺图片，多个图片以','隔开 */
    private String images;

    /** 商圈，例如陆家嘴 */
    private String area;

    /** 地址 */
    private String address;

    /** 经度 */
    private Double x;

    /** 维度 */
    private Double y;

    /** 均价，取整数 */
    private String avgPrice;

    /** 销量 */
    private Integer sold;

    /** 评论数量 */
    private Integer reviews;

    /** 评分，1~5分，乘10保存，避免小数 */
    private Integer score;

    /** 营业时间，例如 10:00-22:00 */
    private String openHours;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Double distance;
    //查找关键字
    private String keyword;

    private Boolean isFollow;
}
