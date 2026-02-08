package com.smartLive.order.api.DTO;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class VoucherOrderDTO extends BaseEntity  implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 下单的用户id */
    @Excel(name = "下单的用户id")
    private Long userId;

    /** 来源ID（可以是代金券ID、商品ID等） */
    @Excel(name = "来源ID")
    private Long sourceId;

    /** 来源类型 1：代金券；2：商品；3：服务 */
    @Excel(name = "来源类型", readConverterExp = "1=代金券,2=商品,3=服务")
    private Integer sourceType;

    /** 支付方式 1：余额支付；2：支付宝；3：微信 */
    @Excel(name = "支付方式 1：余额支付；2：支付宝；3：微信")
    private Integer payType;

    /** 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款 */
    @Excel(name = "订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款")
    private Integer status;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date payTime;

    /** 核销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "核销时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date useTime;

    /** 退款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "退款时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date refundTime;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createTime;
    /** 店铺id */
    @TableField(exist = false)
    private Long shopId;

}
