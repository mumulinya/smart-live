package com.smartLive.search.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品/优惠券索引文档对象
 * 对应 ES 中的 products 索引，存储商品详情及关联店铺的冗余信息以支持高效聚合搜索。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDoc extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 商品 ID (强制序列化为 String 防止前端长整型精度丢失) */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 所属商铺 ID */
    private String shopId;

    /** 商品名称 (聚合搜索核心字段) */
    private String name;

    /** 营销副标题 */
    private String subTitle;

    /** 商品分类 (1:代金券, 2:团购套餐) */
    private Integer category;

    /** 售卖模式 (0:普通, 1:秒杀) */
    private Integer activityType;

    /** 业务规则配置 JSON */
    private String rulesJson;

    /** 实际售卖价 */
    private BigDecimal price;

    /** 市场指导价 (原价) */
    private BigDecimal originalPrice;

    /** 当前剩余库存 */
    private Integer stock;

    /** 累计销量 (权重排序字段) */
    private Integer sold;

    /** 累计评论数 */
    private Integer reviews;

    /** 关注数/粉丝数 */
    private Integer fans;

    /** 收藏总数 */
    private Integer stars;
    
    /** 上架状态 (0:下架, 1:正常, 2:待审核) */
    private Integer status;

    /** 有效期模式 (1:固定日期, 2:动态天数) */
    private Integer validityType;

    /** 使用开始时间 (固定日期模式) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useStartTime;
    
    /** 使用截止时间 (固定日期模式) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useEndTime;

    /** 领取后有效天数 (动态模式) */
    private Integer validDays;

    /** 商品主图 URL */
    private String coverImg;

    /** 秒杀抢购开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /** 秒杀抢购结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    // --- 关联冗余字段 ---
    
    /** 冗余存储店铺名称 */
    private String shopName;
    
    /** 冗余存储店铺 Logo */
    private String shopLogo;
    
    /** 冗余存储店铺图片 */
    private String shopImages;
    
    /** 当前用户是否已收藏 */
    private Boolean isStar;
    
    /** 当前用户是否已关注该店 */
    private Boolean IsFollow;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    /** 店铺分类 ID */
    private Long typeId;
    
    /** 异步操作类型 */
    private String actionType;
    
    /** 业务类型标识 */
    private Integer sourceType;
    
    /** 原始业务 ID */
    private Long sourceId;
}

