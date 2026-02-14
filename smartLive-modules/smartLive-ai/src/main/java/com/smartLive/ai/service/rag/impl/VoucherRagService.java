package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.VoucherVO;
import com.smartLive.ai.service.rag.IVoucherRagService;
import com.smartLive.marketing.api.RemoteVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class VoucherRagService implements IVoucherRagService {

    private final VectorStore voucherVectorStore;
    private final RemoteVoucherService remoteVoucherService;
    @Autowired
    public VoucherRagService(@Qualifier("voucherVectorStore") VectorStore vectorStore,
                             RemoteVoucherService remoteVoucherService) {
        this.voucherVectorStore = vectorStore;
        this.remoteVoucherService = remoteVoucherService;
    }

    @Override
    public List<VoucherVO> getVoucherList(VoucherVO voucherVo, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank()) ? "voucher coupon" : userMessage;
        String filter = buildFilterExpression(voucherVo);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(ragQuery)
                .topK(5);
        if (StringUtils.hasText(filter)) {
            builder.filterExpression(filter);
        }

        List<Document> results = voucherVectorStore.similaritySearch(builder.build());
        log.info("Voucher RAG search results: {}", results);
        return convertDocumentsToVoucherVO(results);
    }

    @Override
    public String orderVoucher(VoucherVO voucherVo) {
        Long userId = voucherVo.getUserId();
        List<Document> results = voucherVectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression(buildFilterExpression(voucherVo))
                        .topK(1)
                        .build());
        log.info("🔍 RAG搜索结果：{}", results);
        //要下单的优惠券
        VoucherVO vo = convertDocumentToVoucherVO(results.get(0));
        if (vo == null) {
            return "没有找到该优惠券";
        }
        log.info("下单的优惠券为" + vo);
        if (vo.getType() == 1 && vo.getStock() <= 0) {
            log.info("该优惠券已售罄");
            return "该优惠券已售罄";
        }
        try {
            Long result = null;
            //普通券
            if (vo.getType() == 0) {
                CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                    Long re = remoteVoucherService.buyVoucher(vo.getId(), userId);
                    return re;
                });
                result = future.get();
                if (result != null ) {
                    return "抢购成功，订单id为" + result;
            } else {
                    return  "抢购失败";
                }
            }
            //秒杀券
            else if (vo.getType() == 1) {
                CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                    Long re = remoteVoucherService.seckillVoucher(vo.getId(), userId);
                    return re;
                });
                result = future.get();
                if (result != null ) {
                    return "抢购成功，订单id为" + result;
                } else {
                    return  "抢购失败";
                }
            }
        } catch (Exception e) {
            log.error("抢购失败", e);
            return "抢购失败";
        }
        return "抢购失败";
    }

    private String buildOrderQuery(VoucherVO voucherVo, String userMessage) {
        List<String> terms = new ArrayList<>();

        if (voucherVo != null) {
            if (StringUtils.hasText(voucherVo.getShopName())) {
                terms.add(voucherVo.getShopName());
            }
            if (StringUtils.hasText(voucherVo.getTitle())) {
                terms.add(voucherVo.getTitle());
            }
        }

        if (StringUtils.hasText(userMessage)) {
            terms.add(userMessage);
        }

        if (terms.isEmpty()) {
            return "voucher order";
        }
        return String.join(" ", terms);
    }

    private VoucherVO pickOrderTarget(List<VoucherVO> candidates, VoucherVO query) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (query == null) {
            return candidates.get(0);
        }

        if (query.getId() != null) {
            for (VoucherVO candidate : candidates) {
                if (query.getId().equals(candidate.getId())) {
                    return candidate;
                }
            }
        }

        if (query.getShopId() != null) {
            for (VoucherVO candidate : candidates) {
                if (query.getShopId().equals(candidate.getShopId())) {
                    return candidate;
                }
            }
        }

        if (StringUtils.hasText(query.getTitle())) {
            for (VoucherVO candidate : candidates) {
                if (query.getTitle().equalsIgnoreCase(candidate.getTitle())) {
                    return candidate;
                }
            }
            for (VoucherVO candidate : candidates) {
                if (containsIgnoreCase(candidate.getTitle(), query.getTitle())) {
                    return candidate;
                }
            }
        }

        if (StringUtils.hasText(query.getShopName())) {
            for (VoucherVO candidate : candidates) {
                if (query.getShopName().equalsIgnoreCase(candidate.getShopName())) {
                    return candidate;
                }
            }
            for (VoucherVO candidate : candidates) {
                if (containsIgnoreCase(candidate.getShopName(), query.getShopName())) {
                    return candidate;
                }
            }
        }

        return candidates.get(0);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return text.toLowerCase().contains(keyword.toLowerCase());
    }

    private String buildFilterExpression(VoucherVO voucherVO) {
        List<String> filters = new ArrayList<>();
        filters.add("status == 1");

        if (voucherVO != null) {
            if (StringUtils.hasText(voucherVO.getShopName())) {
                filters.add(String.format("shopName == '%s'", escapeForFilter(voucherVO.getShopName())));
            }
            if (voucherVO.getType() != null) {
                filters.add("type == " + voucherVO.getType());
            }
            if (voucherVO.getTypeId() != null) {
                filters.add("typeId == " + voucherVO.getTypeId());
            }
            if (StringUtils.hasText(voucherVO.getTitle())) {
                filters.add(String.format("title == '%s'", escapeForFilter(voucherVO.getTitle())));
            }
            if (voucherVO.getId() != null) {
                filters.add("id == " + voucherVO.getId());
            }
            if (voucherVO.getShopId() != null) {
                filters.add("shopId == " + voucherVO.getShopId());
            }
        }
        return String.join(" && ", filters);
    }

    private String escapeForFilter(String input) {
        return input.replace("'", "\\'");
    }

    private List<VoucherVO> convertDocumentsToVoucherVO(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .map(this::convertDocumentToVoucherVO)
                .filter(Objects::nonNull)
                .toList();
    }

    private VoucherVO convertDocumentToVoucherVO(Document document) {
        try {
            Map<String, Object> metadata = document.getMetadata();
            VoucherVO voucher = new VoucherVO();

            voucher.setId(toLong(metadata.get("id")));
            voucher.setShopId(toLong(metadata.get("shopId")));
            voucher.setShopName(toStringValue(metadata.get("shopName")));
            voucher.setTypeId(toLong(metadata.get("typeId")));
            voucher.setTitle(toStringValue(metadata.get("title")));
            voucher.setSubTitle(toStringValue(metadata.get("subTitle")));
            voucher.setRules(toStringValue(metadata.get("rules")));
            voucher.setPayValue(toStringValue(metadata.get("payValue")));
            voucher.setActualValue(toLong(metadata.get("actualValue")));
            voucher.setType(toInteger(metadata.get("type")));
            voucher.setStatus(toInteger(metadata.get("status")));
            voucher.setStock(toInteger(metadata.get("stock")));
            voucher.setBeginTime(toLocalDateTime(metadata.get("beginTime")));
            voucher.setEndTime(toLocalDateTime(metadata.get("endTime")));

            return voucher;
        } catch (Exception e) {
            log.warn("Failed to convert Document to VoucherVO: {}", e.getMessage());
            return null;
        }
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private Long toLong(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.longValue();
    }

    private Integer toInteger(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            log.warn("Cannot parse numeric metadata value: {}", text);
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }

        BigDecimal decimal = toBigDecimal(value);
        if (decimal != null) {
            long epoch = decimal.longValue();
            if (Math.abs(epoch) < 100_000_000_000L) {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
        }

        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault());
        } catch (DateTimeParseException ex) {
            log.warn("Cannot parse time metadata value: {}", text);
            return null;
        }
    }
}
