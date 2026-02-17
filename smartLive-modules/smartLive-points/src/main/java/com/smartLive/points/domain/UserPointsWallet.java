package com.smartLive.points.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户积分钱包
 *
 * @author smartLive
 */
@Data
@TableName("user_points_wallet")
public class UserPointsWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(type = IdType.INPUT)
    private Long userId;

    /** 当前可用积分余额 */
    private Integer balance;

    /** 历史累计获取积分 */
    private Integer totalEarned;

    /** 连续签到天数 */
    private Integer consecutiveDays;

    /** 最后签到日期 */
    private LocalDate lastSignDate;

    /** 乐观锁版本号 */
    private Integer version;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
