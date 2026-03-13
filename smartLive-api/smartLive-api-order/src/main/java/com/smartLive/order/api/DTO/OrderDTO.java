package com.smartLive.order.api.DTO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;

/**
 * 优惠券订单表对象 tb_voucher_order
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class OrderDTO extends BaseEntity  implements Serializable
{
    private static final long serialVersionUID = 1L;
    /** 主键 */
    private Long id;

    private Long userId;

    /** 来源ID（可以是代金券ID、商品ID等） */
    private Long sourceId;

    /** 来源类型 1：代金券；2：商品；3：服务 */
    private Integer sourceType;

    /** 支付方式 1：余额支付；2：支付宝；3：微信 */
    private Integer payType;

    /** 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款 */
    private Integer status;

    // ========== 新增核心交易字段 Start ==========

    /** 实付金额 (单位: 分) */
    private BigDecimal payAmount;

    /** 购买数量 */
    private Integer amount;

    /** 第三方支付流水号 */
    private String outTradeNo;

    // ========== 评价相关字段 (Comment 改为 Review) Start ==========

    /** 评价状态 0：未评价；1：已评价 */
    private Integer reviewStatus;

    /** 评价ID */
    private Long reviewId;

    /** 评价时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    // ========== 时间与退款字段 Start ==========

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 核销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useTime;

    /** 可使用的店铺ID列表（逗号分隔） */
    private String shopId;

    /** 实际核销门店ID */
    private Long verifyShopId;

    /** 退款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date refundTime;

    /** 退款金额 (单位: 分) */
    private Long refundAmount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 订单有效期开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validStartTime;

    /** 订单有效期截止时间（过期作废或被退款） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

}
