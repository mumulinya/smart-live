package com.smartLive.wallet.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartLive.common.core.constant.PaymentStatusConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 支付流水记录
 *
 * @author smartLive
 */
@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付流水号 */
    private String paySn;

    /** 用户ID */
    private Long userId;

    /** 业务类型: recharge(充值), order(订单) */
    private String bizType;

    /** 业务ID */
    private String bizId;

    /** 支付金额(元) */
    private BigDecimal amount;

    /** 支付方式: wechat/alipay */
    private String payMethod;

    /**
     * 支付状态
     * {@link PaymentStatusConstants#PENDING} 待支付
     * {@link PaymentStatusConstants#SUCCESS} 支付成功
     * {@link PaymentStatusConstants#FAILED} 支付失败
     * {@link PaymentStatusConstants#CANCELED} 已取消/已关闭
     */
    private Integer status;

    /** 第三方支付单号 */
    private String transactionId;

    /** 支付成功时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
