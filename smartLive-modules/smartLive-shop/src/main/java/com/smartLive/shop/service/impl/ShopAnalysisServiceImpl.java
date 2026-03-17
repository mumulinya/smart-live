package com.smartLive.shop.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.interaction.api.DTO.ShopReviewAnalysisDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import com.smartLive.order.api.DTO.ProductSalesDTO;
import com.smartLive.order.api.DTO.ShopOrderAnalysisDTO;
import com.smartLive.order.api.DTO.ShopOrderSuggestDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.domain.ShopAnalysisRecord;
import com.smartLive.shop.domain.VO.ProductSalesVO;
import com.smartLive.shop.domain.VO.ShopAnalysisVO;
import com.smartLive.shop.mapper.ShopAnalysisRecordMapper;
import com.smartLive.shop.service.IShopAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShopAnalysisServiceImpl extends ServiceImpl<ShopAnalysisRecordMapper, ShopAnalysisRecord>
        implements IShopAnalysisService {

    private static final DateTimeFormatter SHOP_ANALYSIS_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RemoteOrderService remoteOrderService;

    @Autowired
    private RemoteReviewService remoteReviewService;

    @Autowired
    private ExecutorService executorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopAnalysisVO getShopAnalysis(Long shopId, String timeRange) {
        log.info("shop analysis timeRange:{}", timeRange);
        String normalizedTimeRange = normalizeShopAnalysisTimeRange(timeRange);
        ShopAnalysisRecord existingRecord = findTodayRecord(shopId, normalizedTimeRange);
        if (existingRecord != null) {
            log.info("reuse shop analysis record, shopId={}, timeRange={}, recordId={}",
                    shopId, normalizedTimeRange, existingRecord.getId());
            return toShopAnalysisVO(existingRecord);
        }
        LocalDateTime[] range = buildShopAnalysisRange(normalizedTimeRange);
        String startTime = formatShopAnalysisTime(range[0]);
        String endTime = formatShopAnalysisTime(range[1]);
        CompletableFuture<ShopOrderAnalysisDTO> orderAnalysisFuture = supplyAsync(
                () -> remoteOrderService.getShopOrderAnalysis(shopId, startTime, endTime),
                new ShopOrderAnalysisDTO(),
                "query shop order analysis");
        CompletableFuture<ShopReviewAnalysisDTO> reviewAnalysisFuture = supplyAsync(
                () -> remoteReviewService.getShopReviewAnalysis(shopId, startTime, endTime),
                new ShopReviewAnalysisDTO(),
                "query shop review analysis");
        CompletableFuture<ShopOrderSuggestDTO> orderSuggestFuture = supplyAsync(
                () -> remoteOrderService.getShopOrderSuggest(shopId, normalizedTimeRange),
                new ShopOrderSuggestDTO(),
                "query shop order suggest");

        CompletableFuture.allOf(orderAnalysisFuture, reviewAnalysisFuture, orderSuggestFuture).join();

        ShopOrderAnalysisDTO orderAnalysis = orderAnalysisFuture.join();
        ShopReviewAnalysisDTO reviewAnalysis = reviewAnalysisFuture.join();
        ShopOrderSuggestDTO orderSuggest = orderSuggestFuture.join();
        if (isEmptyAnalysisResult(orderAnalysis, reviewAnalysis, orderSuggest)) {
            log.info("skip saving empty shop analysis record, shopId={}, timeRange={}", shopId, normalizedTimeRange);
            return buildEmptyShopAnalysis();
        }
        ShopAnalysisRecord record = buildShopAnalysisRecord(shopId, normalizedTimeRange, range, orderAnalysis, reviewAnalysis, orderSuggest);
        this.save(record);
        return toShopAnalysisVO(record);
    }

    @Override
    public ShopAnalysisVO getShopAnalysisRecord(Long analysisRecordId, Long shopId) {
        ShopAnalysisRecord record = getAndCheckRecord(analysisRecordId);
        if (shopId == null || !Objects.equals(record.getShopId(), shopId)) {
            throw new BusinessException("analysis record not found");
        }
        return toShopAnalysisVO(record);
    }

    private ShopAnalysisRecord getAndCheckRecord(Long analysisRecordId) {
        if (analysisRecordId == null) {
            throw new BusinessException("analysisRecordId cannot be blank");
        }
        ShopAnalysisRecord record = this.getById(analysisRecordId);
        if (record == null) {
            throw new BusinessException("analysis record not found");
        }
        return record;
    }

    private ShopAnalysisRecord findTodayRecord(Long shopId, String timeRange) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        return this.lambdaQuery()
                .eq(ShopAnalysisRecord::getShopId, shopId)
                .eq(ShopAnalysisRecord::getTimeRange, timeRange)
                .ge(ShopAnalysisRecord::getCreateTime, toDate(todayStart))
                .lt(ShopAnalysisRecord::getCreateTime, toDate(tomorrowStart))
                .orderByDesc(ShopAnalysisRecord::getCreateTime)
                .orderByDesc(ShopAnalysisRecord::getId)
                .last("limit 1")
                .one();
    }

    private LocalDateTime[] buildShopAnalysisRange(String timeRange) {
        String normalized = normalizeShopAnalysisTimeRange(timeRange);
        LocalDateTime now = LocalDateTime.now();
        switch (normalized) {
            case "week":
                return new LocalDateTime[]{now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(), now};
            case "month":
                return new LocalDateTime[]{now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now};
            case "quarter":
                return new LocalDateTime[]{now.minusDays(90), now};
            default:
                throw new BusinessException("unsupported timeRange");
        }
    }

    private String normalizeShopAnalysisTimeRange(String timeRange) {
        String normalized = StrUtil.isBlank(timeRange) ? "week" : timeRange.trim().toLowerCase(Locale.ROOT);
        if ("week".equals(normalized) || "month".equals(normalized) || "quarter".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException("unsupported timeRange");
    }

    private String formatShopAnalysisTime(LocalDateTime time) {
        return time.format(SHOP_ANALYSIS_TIME_FORMATTER);
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

    private boolean isEmptyAnalysisResult(ShopOrderAnalysisDTO orderAnalysis,
                                          ShopReviewAnalysisDTO reviewAnalysis,
                                          ShopOrderSuggestDTO orderSuggest) {
        boolean emptyOrderAnalysis = orderAnalysis == null
                || (isZero(orderAnalysis.getTotalOrders())
                && isZero(orderAnalysis.getTotalRevenue())
                && isZero(orderAnalysis.getRepurchaseCount())
                && isEmptyProductSales(orderAnalysis.getHotProducts()));
        boolean emptyReviewAnalysis = reviewAnalysis == null
                || (isZero(reviewAnalysis.getAvgScore())
                && isZero(reviewAnalysis.getBadReviewCount()));
        boolean emptyOrderSuggest = orderSuggest == null
                || (isZero(orderSuggest.getWeekOrders())
                && isEmptyProductSales(orderSuggest.getHotProducts())
                && isEmptyProductSales(orderSuggest.getSlowProducts()));
        return emptyOrderAnalysis && emptyReviewAnalysis && emptyOrderSuggest;
    }

    private ShopAnalysisRecord buildShopAnalysisRecord(Long shopId,
                                                       String timeRange,
                                                       LocalDateTime[] range,
                                                       ShopOrderAnalysisDTO orderAnalysis,
                                                       ShopReviewAnalysisDTO reviewAnalysis,
                                                       ShopOrderSuggestDTO orderSuggest) {
        List<ProductSalesVO> hotProducts = orderSuggest == null ? new ArrayList<>() : toProductSalesVOList(orderSuggest.getHotProducts());
        List<ProductSalesVO> slowProducts = orderSuggest == null ? new ArrayList<>() : toProductSalesVOList(orderSuggest.getSlowProducts());
        ShopAnalysisRecord record = new ShopAnalysisRecord();
        record.setShopId(shopId);
        record.setTimeRange(timeRange);
        record.setStartTime(toDate(range[0]));
        record.setEndTime(toDate(range[1]));
        record.setTotalOrders(orderAnalysis == null || orderAnalysis.getTotalOrders() == null ? 0 : orderAnalysis.getTotalOrders());
        record.setTotalRevenue(orderAnalysis == null || orderAnalysis.getTotalRevenue() == null ? java.math.BigDecimal.ZERO : orderAnalysis.getTotalRevenue());
        record.setAvgScore(normalizeOneDecimal(reviewAnalysis == null ? null : reviewAnalysis.getAvgScore()));
        record.setBadReviewCount(reviewAnalysis == null || reviewAnalysis.getBadReviewCount() == null ? 0 : reviewAnalysis.getBadReviewCount());
        record.setHotProducts(JSONUtil.toJsonStr(hotProducts));
        record.setSlowProducts(JSONUtil.toJsonStr(slowProducts));
        record.setCreateTime(new Date());
        return record;
    }

    private ShopAnalysisVO toShopAnalysisVO(ShopAnalysisRecord record) {
        if (record == null) {
            return buildEmptyShopAnalysis();
        }
        ShopAnalysisVO result = new ShopAnalysisVO();
        result.setId(record.getId());
        result.setTotalOrders(record.getTotalOrders());
        result.setTotalRevenue(record.getTotalRevenue());
        result.setAvgScore(record.getAvgScore());
        result.setBadReviewCount(record.getBadReviewCount());
        result.setHotProducts(parseProductSales(record.getHotProducts()));
        result.setSlowProducts(parseProductSales(record.getSlowProducts()));
        normalizeShopAnalysis(result);
        return result;
    }

    private java.math.BigDecimal normalizeOneDecimal(java.math.BigDecimal value) {
        if (value == null) {
            return java.math.BigDecimal.ZERO;
        }
        return value.setScale(1, java.math.RoundingMode.HALF_UP);
    }

    private boolean isZero(Number value) {
        return value == null || value.doubleValue() == 0D;
    }

    private boolean isZero(java.math.BigDecimal value) {
        return value == null || value.compareTo(java.math.BigDecimal.ZERO) == 0;
    }

    private List<ProductSalesVO> toProductSalesVOList(List<ProductSalesDTO> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .map(item -> new ProductSalesVO(
                        item.getProductId(),
                        item.getProductName() == null ? "" : item.getProductName(),
                        item.getSalesCount() == null ? 0L : item.getSalesCount()))
                .collect(Collectors.toList());
    }

    private boolean isEmptyProductSales(List<ProductSalesDTO> source) {
        if (source == null || source.isEmpty()) {
            return true;
        }
        return source.stream().filter(Objects::nonNull).findAny().isEmpty();
    }

    private List<ProductSalesVO> parseProductSales(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, ProductSalesVO.class);
        } catch (Exception ex) {
            log.warn("Parse shop analysis products failed, error={}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    private Date toDate(LocalDateTime time) {
        return time == null ? null : java.sql.Timestamp.valueOf(time);
    }

    private ShopAnalysisVO buildEmptyShopAnalysis() {
        return new ShopAnalysisVO(null, 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0, new ArrayList<>(), new ArrayList<>());
    }

    private void normalizeShopAnalysis(ShopAnalysisVO result) {
        if (result.getTotalOrders() == null) {
            result.setTotalOrders(0);
        }
        if (result.getTotalRevenue() == null) {
            result.setTotalRevenue(java.math.BigDecimal.ZERO);
        }
        if (result.getAvgScore() == null) {
            result.setAvgScore(java.math.BigDecimal.ZERO);
        }
        if (result.getBadReviewCount() == null) {
            result.setBadReviewCount(0);
        }
        if (result.getHotProducts() == null) {
            result.setHotProducts(new ArrayList<>());
        }
        if (result.getSlowProducts() == null) {
            result.setSlowProducts(new ArrayList<>());
        }
    }
}
