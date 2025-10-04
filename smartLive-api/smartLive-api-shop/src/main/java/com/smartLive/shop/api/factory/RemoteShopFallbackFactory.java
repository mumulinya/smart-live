package com.smartLive.shop.api.factory;
import com.smartLive.common.core.domain.R;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteShopFallbackFactory implements FallbackFactory<RemoteShopService> {

    @Override
    public RemoteShopService create(Throwable cause) {
        return new RemoteShopService() {
            @Override
            public R<ShopDTo> getShopByShopName(String shopName) {
                return R.fail("查询商家信息失败");
            }

            /**
             * 更新商家评论数
             *
             * @param shopId
             */
            @Override
            public R<Boolean> updateCommentById(Long shopId) {
                return  R.fail("更新商家评论数失败");
            }
        };
    }
}
