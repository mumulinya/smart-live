package com.smartLive.order.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单返回对象 (View Object)
 * 用于前端展示订单详情
 * * @author mumulin
 * @date 2025-09-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 订单ID (防止前端精度丢失，序列化为String) */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 下单的用户id */
    private Long userId;

    /** 来源ID（可以是代金券ID、商品ID等） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceId;

    /** 来源类型 1：代金券；2：商品；3：服务 */
    private Integer sourceType;

    /** 代金券标题 */
    private String title;

    /** 副标题 */
    private String subTitle;

    /** * 代金券单价/原价
     * (注意：这是券的面额或单价，不是订单总实付)
     */
    private BigDecimal payValue;

    /** 抵扣金额，单位是分 */
    private BigDecimal actualValue;

    // ========== 新增核心展示字段 ==========

    /** * 订单实付总金额 (单位: 分)
     * (新增：展示用户实际支付了多少钱)
     */
    private BigDecimal payAmount;
    /** * 订单封面图片 */
    private String coverImg;
    /** * 评价状态
     * 0：未评价；1：已评价
     * (新增：用于前端判断显示"去评价"还是"查看评价"按钮)
     */
    private Integer reviewStatus;

    /** * 评价ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewId;

    /** * 评价时间
     * (新增：展示评价日期)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    /**
     * 退款金额 (单位: 分)
     * (新增：如果有退款，展示退了多少)
     */
    private BigDecimal refundAmount;

    // ====================================

    /** 支付方式 1：余额支付；2：支付宝；3：微信 */
    private Integer payType;

    /** 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款 */
    private Integer status;

    /** 使用规则 */
    private String rules;

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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 订单有效期开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validStartTime;

    /** 订单有效期截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;
}
