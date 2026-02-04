package com.smartLive.marketing.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 优惠券返回对象 (View Object)
 * 用于前端展示优惠券详情
 * 
 * @author 木木林
 * @date 2025-09-21
 */
@Data
public class VoucherVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 商铺id */
    private Long shopId;

    /** 代金券标题 */
    private String title;

    /** 副标题 */
    private String subTitle;

    /** 使用规则 */
    private String rules;

    /** 支付金额，单位是分。例如200代表2元 */
    private String payValue;

    /** 抵扣金额，单位是分。例如200代表2元 */
    private Long actualValue;

    /** 0,普通券；1,秒杀券 */
    private Integer type;

    /** 1,上架; 2,下架; 3,过期 */
    private Integer status;
    
    /** 销量*/
    private Integer sold;
    
    //评价数
    private Integer reviews;
    
    //粉丝数
    private Integer fans;
    
    //收藏数
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
     * 库存
     */
    private Integer stock;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    
    //店铺名称
    private String shopName;
    
    //店铺类型
    private Long typeId;
    
    //店铺logo
    private String shopLogo;
    
    //店铺图片
    private String shopImages;
    
    //是否收藏
    private Boolean isStar;
    
    //是否关注
    private Boolean IsFollow;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}