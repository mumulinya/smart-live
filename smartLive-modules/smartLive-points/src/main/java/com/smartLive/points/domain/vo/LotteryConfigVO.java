package com.smartLive.points.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 抽奖配置VO
 *
 * @author smartLive
 */
@Data
public class LotteryConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 每次抽奖消耗积分 */
    private Integer costPerDraw;

    /** 奖品列表 */
    private List<PrizeItem> prizes;

    @Data
    public static class PrizeItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 奖品ID */
        private Integer id;
        /** 奖品名称 */
        private String name;
        /** 奖品类型: points/coupon */
        private String type;
        /** 奖品价值 */
        private Integer value;
        /** 描述 */
        private String desc;
    }
}
