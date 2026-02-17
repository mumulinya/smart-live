package com.smartLive.points.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分抽奖商品配置对象 points_lottery_prize
 *
 * @author smartLive
 */
@Data
@TableName("points_lottery_prize")
public class PointsLotteryPrize implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 奖品名称 */
    private String name;

    /** 奖品类型 (points:积分, coupon:优惠券, none:未中奖) */
    private String type;

    /** 奖品价值 (积分数或优惠券ID) */
    private Integer value;

    /** 中奖概率权重 */
    private Integer probability;

    /** 描述 */
    private String description;

    /** 状态 (1:启用, 0:禁用) */
    private Integer status;

    /** 排序 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
