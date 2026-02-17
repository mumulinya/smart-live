package com.smartLive.points.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员手动调整积分DTO
 *
 * @author smartLive
 */
@Data
public class PointsAdjustDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 类型 1:增加(充值) 2:扣减 */
    private Integer type;

    /** 积分数量 */
    private Integer amount;

    /** 备注原因 */
    private String reason;
}
