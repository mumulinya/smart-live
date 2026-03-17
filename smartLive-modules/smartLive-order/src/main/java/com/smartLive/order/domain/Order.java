package com.smartLive.order.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;

/**
 * 订单表实体对象，对应表 tb_order。
 *
 * @author mumulin
 * @date 2025-09-21
 */
@TableName("`order`")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 下单的用户ID */
    @Excel(name = "下单的用户id")
    private Long userId;

    /** 来源ID（可以是代金券ID、商品ID等） */
    @Excel(name = "来源ID")
    private Long sourceId;

    /** 来源类型 1：代金券；2：商品；3：服务 */
    @Excel(name = "来源类型", readConverterExp = "1=代金券,2=商品,3=服务")
    private Integer sourceType;

    /** 支付方式 1：余额支付；2：支付宝；3：微信 */
    @Excel(name = "支付方式", readConverterExp = "1=余额支付,2=支付宝,3=微信")
    private Integer payType;

    /** 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款 */
    @Excel(name = "订单状态", readConverterExp = "1=未支付,2=已支付,3=已核销,4=已取消,5=退款中,6=已退款")
    private Integer status;

    // ========== 新增核心交易字段 开始 ==========

    /** 实付金额 (单位: 分) */
    @Excel(name = "实付金额(分)")
    private BigDecimal payAmount;

    /** 购买数量 */
    @Excel(name = "购买数量")
    private Integer amount;

    /** 第三方支付流水号 */
    @Excel(name = "第三方支付流水号")
    private String outTradeNo;

    // ========== 评价相关字段（评论字段调整为评价字段）开始 ==========

    /** 评价状态 0：未评价；1：已评价 */
    @Excel(name = "评价状态", readConverterExp = "0=未评价,1=已评价")
    private Integer reviewStatus;

    /** 评价ID */
    @Excel(name = "评价ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewId;

    /** 评价时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "评价时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date reviewTime;

    // ========== 时间与退款字段 开始 ==========

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "支付时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date payTime;

    /** 核销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "核销时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date useTime;

    /** 可使用的店铺ID列表（逗号分隔） */
    @Excel(name = "可使用店铺ID列表")
    private String shopId;

    /** 实际核销门店ID */
    @Excel(name = "实际核销门店ID")
    private Long verifyShopId;

    /** 退款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "退款时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date refundTime;

    /** 退款金额 (单位: 分) */
    @Excel(name = "退款金额(分)")
    private Long refundAmount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createTime;

    /** 订单有效期开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "有效期开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date validStartTime;

    /** 订单有效期截止时间（过期作废或被退款） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "有效期截止时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireTime;

    /** 查询条件：店铺ID集合 */
    @TableField(exist = false)
    private List<Long> shopIds;

    /** 查询条件：排除状态集合 */
    @TableField(exist = false)
    private List<Integer> excludedStatuses;
}
