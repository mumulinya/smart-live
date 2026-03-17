package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ShopVO;

import java.util.List;

/**
 * 店铺 RAG 服务
 */
public interface IShopRagService {
    /**
     * 获取店铺列表。
     *
     * @param shopVo 店铺查询条件
     * @param userMessage 用户问题文本
     * @return 店铺列表
     */
    List<ShopVO> getShopList(ShopVO shopVo, String userMessage);
    /**
     * 获取店铺详情。
     *
     * @param shopVo 店铺查询条件
     * @param userMessage 用户问题文本
     * @return 店铺详情
     */
    ShopVO getShopDetails(ShopVO shopVo, String userMessage);
}
