package com.smartLive.points.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 积分流水记录VO
 *
 * @author smartLive
 */
@Data
public class PointsRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long id;

    /** 时间 */
    private String date;

    /** 描述文案 */
    private String desc;

    /** 变动金额 */
    private Integer value;

    /** 类型 in:收入 out:支出 */
    private String type;
}
