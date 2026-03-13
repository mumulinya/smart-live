package com.smartLive.shop.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 店铺返回对象 (View Object)
 * 用于前端展示店铺详情
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class ShopVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 商铺名称 */
    private String name;

    /** 商铺类型的id */
    private Long typeId;

    /** 商铺图片，多个图片以','隔开 */
    private String images;

    /** 店铺头像 */
    private String shopLogo;

    /** 商圈，例如陆家嘴 */
    private String area;

    /** 地址 */
    private String address;

    /** 经度 */
    private Double x;

    /** 维度 */
    private Double y;

    /** 均价，取整数 */
    private Integer avgPrice;

    /** 销量 */
    private Integer sold;

    /** 评论数量 */
    private Integer reviews;
    
    /** 收藏数量 */
    private Integer stared;
    
    /** fans count */
    private Integer fans;

    /** 评分，1~5分，乘10保存，避免小数 */
    private Integer score;

    /** 营业时间，例如 10:00-22:00 */
    private String openHours;

    /** 审核状态 */
    private Integer status;

    private Integer auditStatus;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private Double distance;
    private String location;
    
    /** 是否收藏 */
    private Boolean isStared;

    /** 是否关注 */
    private Boolean isFollowed;
}
