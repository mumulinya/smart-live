package com.smartLive.product.api.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品传输对象 (DTO)
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class ProductDTO extends BaseEntity implements Serializable {

    /** 主键 */
    private Long id;

    /** 商铺id */
    private String shopId;

    /** 商品名称 */
    private String name;

    /** 副标题 */
    private String subTitle;

    /** 商品类别 1:代金券, 2:团购套餐 */
    private Integer category;

    /** 活动类型 0:普通售卖, 1:秒杀活动 */
    private Integer activityType;

    /** 规则配置 (JSON) */
    private String rulesJson;

    /** 实际售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 状态 */
    private Integer status;

    private Integer auditStatus;

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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 粉丝数 */
    private Integer fans;

    /** 销量 */
    private Integer sold;
}
