package com.smartLive.interaction.service;

/**
 * 独立的热榜积分计算与排行榜洗牌服务。
 * 供定时任务或事件中心调度。
 */
public interface IHotRankService {

    /**
     * 根据指定的业务类型，重算该业务的热榜数据
     *
     * @param bizTypeCode GlobalBizTypeEnum 中的 code 对应的业务线
     */
    void calcHotRankDataByBizType(Integer bizTypeCode);
}
