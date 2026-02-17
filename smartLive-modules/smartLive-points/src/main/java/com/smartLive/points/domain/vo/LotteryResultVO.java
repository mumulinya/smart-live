package com.smartLive.points.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 抽奖结果VO
 *
 * @author smartLive
 */
@Data
public class LotteryResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 中奖奖品ID */
    private Integer prizeId;

    /** 奖品名称 */
    private String prizeName;

    /** 奖品类型: points/coupon */
    private String prizeType;

    /** 奖品价值 */
    private Integer prizeValue;
}
