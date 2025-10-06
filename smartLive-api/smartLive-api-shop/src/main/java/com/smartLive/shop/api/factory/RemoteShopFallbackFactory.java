package com.smartLive.shop.api.factory;
import com.smartLive.common.core.domain.R;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import com.smartLive.shop.api.domain.ShopTypeDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RemoteShopFallbackFactory implements FallbackFactory<RemoteShopService> {

    @Override
    public RemoteShopService create(Throwable cause) {
        return new RemoteShopService() {
            @Override
            public R<ShopDTO> getShopByShopName(String shopName) {
                return R.fail("查询商家信息失败");
            }

            /**
             * 更新商家评论数
             *
             * @param shopId
             */
            @Override
            public R<Boolean> updateCommentById(Long shopId) {
                return R.fail("更新商家评论数失败");
            }

            /**
             * 根据条件查询商家信息
             *
             * @param shopDTo
             */
            @Override
            public R<List<ShopDTO>> queryShopList(ShopDTO shopDTo) {
                return R.fail("查询商家信息失败");
            }

            /**
             * 查询商铺类型列表
             */
            @Override
            public R<List<ShopTypeDTO>> getShopTypeList() {

                return R.fail("查询商铺类型列表失败");
            }

        };
    }
}