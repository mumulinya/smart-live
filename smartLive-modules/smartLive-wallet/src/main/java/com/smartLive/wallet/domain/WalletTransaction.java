package com.smartLive.wallet.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 钱包动账流水记录实体类
 * 对应数据库表 wallet_transaction，记录余额的所有增减审计明细。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@TableName("wallet_transaction")
public class WalletTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 动账类型（1-充值，2-提现，3-消费，4-退款，5-手工调整）
     */
    private Integer type;

    /**
     * 变动金额
     */
    private BigDecimal amount;

    /**
     * 资金流向（1-收入/进账，2-支出/出账）
     */
    private Integer direction;

    /**
     * 变动后的账户余额（快照）
     */
    private BigDecimal balanceAfter;

    /**
     * 关联业务类型（order-订单，recharge-充值，refund-退款）
     */
    private String bizType;

    /**
     * 关联业务单据 ID
     */
    private String bizId;

    /**
     * 流水标题（如：支付宝充值、商品消费）
     */
    private String title;

    /**
     * 处理状态（0-进行中，1-成功，2-失败）
     */
    private Integer status;

    /**
     * 补充备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
