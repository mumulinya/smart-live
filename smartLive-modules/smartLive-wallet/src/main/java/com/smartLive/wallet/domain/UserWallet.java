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
 * 用户个人钱包实体类
 * 对应数据库表 user_wallet，存储用户的余额、冻结资金及交易密码。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@TableName("user_wallet")
public class UserWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID (与 sys_user 关联)
     */
    @TableId(type = IdType.INPUT)
    private Long userId;

    /**
     * 账户可用余额
     */
    private BigDecimal balance;

    /**
     * 冻结金额（如提现中、竞拍中暂扣的资金）
     */
    private BigDecimal frozenBalance;

    /**
     * 钱包支付密码（BCrypt 加密密文）
     */
    private String payPassword;

    /**
     * 钱包状态（1-正常，0-冻结）
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 最近一次动账或配置更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
