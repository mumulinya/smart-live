package com.smartLive.shop.controller;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.shop.service.IShopSuggestService;
import com.smartLive.system.api.RemoteUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shop/suggest")
public class ShopSuggestController extends BaseController {

    private final IShopSuggestService shopSuggestService;
    private final RemoteUserService remoteUserService;

    public ShopSuggestController(IShopSuggestService shopSuggestService,
                                 RemoteUserService remoteUserService) {
        this.shopSuggestService = shopSuggestService;
        this.remoteUserService = remoteUserService;
    }

    @GetMapping("/{shopId}")
    public AjaxResult getShopSuggest(@PathVariable("shopId") Long shopId) {
        validateShopPermission(shopId);
        return success(shopSuggestService.getShopSuggest(shopId));
    }

    private void validateShopPermission(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("shopId cannot be blank");
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException("User not logged in");
        }
        if (SecurityUtils.isAdmin(userId)) {
            return;
        }
        List<Long> shopIds = remoteUserService.getShopIdsByUserId(userId);
        if (CollUtil.isEmpty(shopIds) || !shopIds.contains(shopId)) {
            throw new ServiceException("No permission to access shop data");
        }
    }
}
