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

/**
 * 店铺经营分析服务实现类。
 */
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

    /**
     * 获取店铺经营分析数据。
     */
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

    /**
     * 获取店铺经营分析记录。
     */
    @Override
    public ShopAnalysisVO getShopAnalysisRecord(Long analysisRecordId, Long shopId) {
        ShopAnalysisRecord record = getAndCheckRecord(analysisRecordId);
        if (shopId == null || !Objects.equals(record.getShopId(), shopId)) {
            throw new BusinessException("analysis record not found");
        }
        return toShopAnalysisVO(record);
    }

    /**
     * 查询并校验经营分析记录。
     */
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

    /**
     * 查询当天已生成的经营分析记录。
     */
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

    /**
     * 构建店铺经营分析时间范围。
     */
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

    /**
     * 规范化经营分析时间范围。
     */
    private String normalizeShopAnalysisTimeRange(String timeRange) {
        String normalized = StrUtil.isBlank(timeRange) ? "week" : timeRange.trim().toLowerCase(Locale.ROOT);
        if ("week".equals(normalized) || "month".equals(normalized) || "quarter".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException("unsupported timeRange");
    }

    /**
     * 格式化经营分析时间。
     */
    private String formatShopAnalysisTime(LocalDateTime time) {
        return time.format(SHOP_ANALYSIS_TIME_FORMATTER);
    }

    /**
     * 异步执行任务并提供默认值。
     */
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

    /**
     * 判断经营分析结果是否为空。
     */
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

    /**
     * 构建店铺经营分析记录。
     */
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

    /**
     * 将分析记录转换为经营分析视图对象。
     */
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

    /**
     * 将数值保留一位小数。
     */
    private java.math.BigDecimal normalizeOneDecimal(java.math.BigDecimal value) {
        if (value == null) {
            return java.math.BigDecimal.ZERO;
        }
        return value.setScale(1, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 判断数值是否为零。
     */
    private boolean isZero(Number value) {
        return value == null || value.doubleValue() == 0D;
    }

    /**
     * 判断数值是否为零。
     */
    private boolean isZero(java.math.BigDecimal value) {
        return value == null || value.compareTo(java.math.BigDecimal.ZERO) == 0;
    }

    /**
     * 将商品销量数据转换为视图对象列表。
     */
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

    /**
     * 判断商品销量列表是否为空。
     */
    private boolean isEmptyProductSales(List<ProductSalesDTO> source) {
        if (source == null || source.isEmpty()) {
            return true;
        }
        return source.stream().filter(Objects::nonNull).findAny().isEmpty();
    }

    /**
     * 解析商品销量数据。
     */
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

    /**
     * 将时间转换为日期对象。
     */
    private Date toDate(LocalDateTime time) {
        return time == null ? null : java.sql.Timestamp.valueOf(time);
    }

    /**
     * 构建空的经营分析结果。
     */
    private ShopAnalysisVO buildEmptyShopAnalysis() {
        return new ShopAnalysisVO(null, 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * 规范化经营分析结果。
     */
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
