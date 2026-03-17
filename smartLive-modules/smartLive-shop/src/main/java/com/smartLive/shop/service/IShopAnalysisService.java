package com.smartLive.shop.service;

import com.smartLive.shop.domain.VO.ShopAnalysisVO;

/**
 * 店铺经营分析服务接口。
 */
public interface IShopAnalysisService {

    /**
     * 获取店铺经营分析数据。
     */
    ShopAnalysisVO getShopAnalysis(Long shopId, String timeRange);

    /**
     * 获取店铺经营分析记录。
     */
    ShopAnalysisVO getShopAnalysisRecord(Long analysisRecordId, Long shopId);
}
