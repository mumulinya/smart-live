package com.smartLive.shop.api.factory;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.DTO.ShopTypeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RemoteShopFallbackFactory implements FallbackFactory<RemoteShopService> {

    @Override
    public RemoteShopService create(Throwable cause) {
        return new RemoteShopService() {
            @Override
            public ShopDTO getShopByShopName(String shopName) {
                log.error("查询商家信息失败:{}", cause.getMessage());
                return null;
            }

            /**
             * 更新商家评论数
             *
             * @param shopId
             */
            @Override
            public Boolean updateCommentById(Long shopId) {
                log.error("更新商家评论数失败:{}", cause.getMessage());
                return false;
            }

            /**
             * 根据条件查询商家信息
             *
             * @param shopDTo
             */
            @Override
            public List<ShopDTO> queryShopList(ShopDTO shopDTo) {
                log.error("查询商家信息失败:{}", cause.getMessage());
                return null;
            }

            /**
             * 查询商铺类型列表
             */
            @Override
            public List<ShopTypeDTO> getShopTypeList() {
                log.error("查询商铺类型列表失败:{}", cause.getMessage());
                return null;
            }

            /**
             * 根据商家Id查询商家信息
             *
             * @param shopId
             */
            @Override
            public ShopDTO getShopById(Long shopId) {
                log.error("查询商家信息失败:{}", cause.getMessage());
                return null;
            }

            @Override
            public List<ShopDTO> getShopList(List<Long> shopIdList) {
                log.error("查询店铺列表失败:{}", cause.getMessage());
                return null;
            }

            /**
             * 获取商家总数
             */
            @Override
            public Integer getShopTotal() {
                log.error("查询商家总数失败:{}", cause.getMessage());
                return null;
            }
            /**
             * 获取最近商家列表
             *
             * @param limit
             */
            @Override
            public List<ShopDTO> getRecentShops(Integer limit) {
                log.error("获取最近商家列表失败:{}", cause.getMessage());
                return null;
            }

            /**
             * 批量更新商家评论数
             *
             * @param updateMap
             */
            @Override
            public Boolean updateCommentCountBatch(Map<Long, Integer> updateMap) {
                log.error("批量更新商家评论数失败:{}", cause.getMessage());
                return false;
            }

            /**
             * 批量更新商家收藏数
             *
             * @param updateMap
             */
            @Override
            public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
                log.error("批量更新商家收藏数失败:{}", cause.getMessage());
                return false;
            }
        };
    }
}