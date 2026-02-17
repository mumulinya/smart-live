package com.smartLive.points.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 钱包信息VO
 *
 * @author smartLive
 */
@Data
public class WalletInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前余额 */
    private Integer balance;

    /** 历史总积分 */
    private Integer totalEarned;

    /** 当前等级名称 */
    private String levelName;

    /** 距离下一级所需积分 */
    private Integer nextLevelNeed;

    /** 当前等级进度百分比 (0-100) */
    private Integer progressPercent;

    /** 今日是否已签到 */
    private Boolean signedIn;

    /** 当前连续签到天数 */
    private Integer consecutiveDays;

    /** 当前等级权益列表 */
    private List<BenefitItem> benefits;

    @Data
    public static class BenefitItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String title;
        private String desc;
    }
}
