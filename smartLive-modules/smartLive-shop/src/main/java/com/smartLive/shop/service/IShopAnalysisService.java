package com.smartLive.shop.service;

import com.smartLive.shop.domain.VO.ShopAnalysisVO;

public interface IShopAnalysisService {

    ShopAnalysisVO getShopAnalysis(Long shopId, String timeRange);

    ShopAnalysisVO getShopAnalysisRecord(Long analysisRecordId, Long shopId);
}
