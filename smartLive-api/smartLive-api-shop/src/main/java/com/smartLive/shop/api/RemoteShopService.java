package com.smartLive.shop.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.shop.api.domain.ShopDTO;
import com.smartLive.shop.api.domain.ShopTypeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(contextId = "remoteShopService", value = ServiceNameConstants.SHOP_SERVICE)
public interface RemoteShopService {
    /**
     * 根据商家名称查询商家信息
     */
    @GetMapping("/shop/{shopName}")
    public R<ShopDTO> getShopByShopName(@PathVariable("shopName") String shopName);
    /**
     * 更新商家评论数
     */
    @PostMapping("/shop/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long shopId);

    /**
     * 根据条件查询商家信息
     */
    @PostMapping("/shop/getShopList")
    public R<List<ShopDTO>> queryShopList(@RequestBody ShopDTO shopDTo);

    /**
     * 查询商铺类型列表
     */
    @GetMapping("/shop-type/getShopListByType")
    public R<List<ShopTypeDTO>> getShopTypeList();
}
