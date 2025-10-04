package com.smartLive.shop.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.shop.api.domain.ShopDTo;
import com.smartLive.shop.api.factory.RemoteShopFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(contextId = "remoteShopService", value = ServiceNameConstants.SHOP_SERVICE)
public interface RemoteShopService {
    /**
     * 根据商家名称查询商家信息
     */
    @GetMapping("/shop/{shopName}")
    public R<ShopDTo> getShopByShopName(@PathVariable("shopName") String shopName);
    /**
     * 更新商家评论数
     */
    @PostMapping("/shop/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long shopId);
}
