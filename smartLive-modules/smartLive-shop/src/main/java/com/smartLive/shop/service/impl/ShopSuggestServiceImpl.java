package com.smartLive.shop.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.ai.api.RemoteAiMerchantService;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.interaction.api.DTO.BadReviewDTO;
import com.smartLive.interaction.api.DTO.ShopReviewSuggestDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import com.smartLive.order.api.DTO.ProductSalesDTO;
import com.smartLive.order.api.DTO.ShopOrderSuggestDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.domain.VO.ProductSalesVO;
import com.smartLive.shop.domain.VO.ShopSuggestVO;
import com.smartLive.shop.service.IShopSuggestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShopSuggestServiceImpl implements IShopSuggestService {

    private static final String SHOP_SUGGEST_TIME_RANGE = "month";

    private final RemoteReviewService remoteReviewService;
    private final RemoteOrderService remoteOrderService;
    private final RemoteAiMerchantService remoteAiMerchantService;
    private final ExecutorService executorService;

    public ShopSuggestServiceImpl(RemoteReviewService remoteReviewService,
                                  RemoteOrderService remoteOrderService,
                                  RemoteAiMerchantService remoteAiMerchantService,
                                  @Qualifier("executorService")  ExecutorService executorService) {
        this.remoteReviewService = remoteReviewService;
        this.remoteOrderService = remoteOrderService;
        this.remoteAiMerchantService = remoteAiMerchantService;
        this.executorService = executorService;
    }

    @Override
    public ShopSuggestVO getShopSuggest(Long shopId) {
        validateShopId(shopId);
        return buildShopSuggest(shopId);
    }

    @Override
    public ShopSuggestVO getShopSuggestInner(Long shopId) {
        validateShopId(shopId);
        return buildShopSuggest(shopId);
    }

    private ShopSuggestVO buildShopSuggest(Long shopId) {
        CompletableFuture<ShopReviewSuggestDTO> reviewSuggestFuture = supplyAsync(
                () -> remoteReviewService.getShopReviewSuggest(shopId, SHOP_SUGGEST_TIME_RANGE),
                new ShopReviewSuggestDTO(),
                "query shop review suggest");
        CompletableFuture<BigDecimal> repurchaseRateFuture = supplyAsync(
                () -> normalizePercent(remoteOrderService.getShopRepurchaseRate(shopId, SHOP_SUGGEST_TIME_RANGE)),
                BigDecimal.ZERO,
                "query shop repurchase rate");
        CompletableFuture<ShopOrderSuggestDTO> orderSuggestFuture = supplyAsync(
                () -> remoteOrderService.getShopOrderSuggest(shopId, SHOP_SUGGEST_TIME_RANGE),
                new ShopOrderSuggestDTO(),
                "query shop order suggest");
        CompletableFuture<List<String>> badReviewKeywordsFuture = reviewSuggestFuture.thenApplyAsync(
                reviewSuggest -> extractKeywords(reviewSuggest == null ? null : reviewSuggest.getBadReviewList()),
                executorService
        ).exceptionally(ex -> {
            log.error("Failed to extract bad review keywords, shopId={}, error={}", shopId, ex.getMessage());
            return new ArrayList<>();
        });

        CompletableFuture.allOf(
                reviewSuggestFuture,
                repurchaseRateFuture,
                orderSuggestFuture,
                badReviewKeywordsFuture
        ).join();

        ShopSuggestVO result = buildEmptyShopSuggest();
        ShopReviewSuggestDTO reviewSuggest = reviewSuggestFuture.join();
        log.info("店铺 review suggest={}", reviewSuggest);
        if (reviewSuggest != null) {
            result.setPendingReviewCount(defaultInteger(reviewSuggest.getPendingReviewCount()));
            result.setBadReviewCount(defaultInteger(reviewSuggest.getBadReviewCount()));
        }
        result.setBadReviewKeywords(badReviewKeywordsFuture.join());
        result.setRepurchaseRate(repurchaseRateFuture.join());
        ShopOrderSuggestDTO orderSuggest = orderSuggestFuture.join();
        result.setSlowProducts(toProductSalesVOList(orderSuggest == null ? null : orderSuggest.getSlowProducts()));
        normalizeShopSuggest(result);
        return result;
    }

    private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, T defaultValue, String taskName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception ex) {
                log.error("Failed to {}, error={}", taskName, ex.getMessage());
                return defaultValue;
            }
        }, executorService);
    }

    private void validateShopId(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("shopId cannot be blank");
        }
    }

    private List<String> extractKeywords(List<BadReviewDTO> badReviewList) {
        if (CollUtil.isEmpty(badReviewList)) {
            return new ArrayList<>();
        }
        List<String> contents = badReviewList.stream()
                .filter(Objects::nonNull)
                .map(BadReviewDTO::getContent)
                .filter(content -> content != null && !content.isBlank())
                .map(String::trim)
                .limit(20)
                .collect(Collectors.toList());
        if (contents.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> keywords;
        try {
            keywords = remoteAiMerchantService.extractKeywords(contents);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
        if (CollUtil.isEmpty(keywords)) {
            return new ArrayList<>();
        }
        Set<String> uniqueKeywords = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            uniqueKeywords.add(keyword.trim());
            if (uniqueKeywords.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(uniqueKeywords);
    }

    private List<ProductSalesVO> toProductSalesVOList(List<ProductSalesDTO> products) {
        if (products == null) {
            return new ArrayList<>();
        }
        return products.stream()
                .filter(Objects::nonNull)
                .map(product -> new ProductSalesVO(
                        product.getProductId(),
                        product.getProductName() == null ? "" : product.getProductName(),
                        product.getSalesCount() == null ? 0L : product.getSalesCount()))
                .collect(Collectors.toList());
    }

    private BigDecimal normalizePercent(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private ShopSuggestVO buildEmptyShopSuggest() {
        return new ShopSuggestVO(0, 0, BigDecimal.ZERO, new ArrayList<>(), new ArrayList<>());
    }

    private void normalizeShopSuggest(ShopSuggestVO result) {
        if (result.getPendingReviewCount() == null) {
            result.setPendingReviewCount(0);
        }
        if (result.getBadReviewCount() == null) {
            result.setBadReviewCount(0);
        }
        if (result.getRepurchaseRate() == null) {
            result.setRepurchaseRate(BigDecimal.ZERO);
        }
        if (result.getSlowProducts() == null) {
            result.setSlowProducts(new ArrayList<>());
        }
        if (result.getBadReviewKeywords() == null) {
            result.setBadReviewKeywords(new ArrayList<>());
        }
    }
}
