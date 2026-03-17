package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ShopVO;

import java.util.List;

/**
 * 店铺 RAG 服务
 */
public interface IShopRagService {
    /**
     * 获取店铺列表
     * @param shopVo
     * @param userMessage
     * @return
     */
    List<ShopVO> getShopList(ShopVO shopVo, String userMessage);
    /**
     * 获取店铺详情
     * @param shopVo
     * @param userMessage
     * @return
     */
    ShopVO getShopDetails(ShopVO shopVo, String userMessage);
}
