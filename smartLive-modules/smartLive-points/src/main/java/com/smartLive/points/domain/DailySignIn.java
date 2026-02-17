package com.smartLive.points.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到记录
 *
 * @author smartLive
 */
@Data
@TableName("daily_sign_in")
public class DailySignIn implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 签到日期 */
    private LocalDate signDate;

    /** 获得积分 */
    private Integer points;

    /** 当前连续签到天数 */
    private Integer consecutiveDays;

    /** 创建时间 */
    private LocalDateTime createTime;
}
