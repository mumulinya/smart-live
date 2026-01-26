package com.smartLive.order.domain.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 优惠券订单返回对象
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherOrderVO extends BaseEntity  implements Serializable
{
    private static final long serialVersionUID = 1L;
    private  Long id;

    /** 下单的用户id */
    private Long userId;

    /** 购买的代金券id */
    private Long voucherId;
    /** 代金券标题 */
    private String title;
    /** 副标题 */
    private String subTitle;
    /** 支付金额，单位是分。例如200代表2元 */
    private String payValue;

    /** 抵扣金额，单位是分。例如200代表2元 */
    private Long actualValue;

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

    /** 退款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date refundTime;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 店铺id */
    private Long shopId;
    /** 店铺名称 */
    private String shopName;
}
