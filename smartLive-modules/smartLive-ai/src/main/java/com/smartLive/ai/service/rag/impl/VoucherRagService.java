package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.VoucherVO;
import com.smartLive.ai.service.rag.IVoucherRagService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.api.RemoteVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    public VoucherRagService(@Qualifier("voucherVectorStore") VectorStore vectorStore, RemoteVoucherService remoteVoucherService) {
        this.voucherVectorStore = vectorStore;
        this.remoteVoucherService = remoteVoucherService;
    }

    /**
     * 获取优惠券列表
     *
     * @param voucherVo
     * @param userMessage
     * @return
     */
    @Override
    public List<VoucherVO> getVoucherList(VoucherVO voucherVo, String userMessage) {
        String ragQuery = userMessage != null ? userMessage : "代金券";
        List<Document> results = voucherVectorStore.similaritySearch(
                SearchRequest.builder()
                .query(ragQuery)
                .topK(5)
                .similarityThreshold(0.6)
                .filterExpression(buildFilterExpression(voucherVo))
                .build());
        // 转换为VoucherVO列表
        List<VoucherVO> vouchers = convertDocumentsToVoucherVO(results);
            log.info("🔍 RAG搜索结果：{}", results);
        return vouchers;
    }
    /**
     * 抢购优惠券
     *
     * @param voucherVo
     * @return
     */
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
        }catch (Exception e){
            log.error("抢购失败", e);
            return "抢购失败";
        }
        return "抢购失败";
    }
    /**
     * 构建过滤条件
     */
    private String buildFilterExpression(VoucherVO voucherVO) {
        List<String> filters = new ArrayList<>();
        // 1. 基础状态过滤（必须条件）
        filters.add("status == "+1); // 只查询上架状态的优惠券
        if(voucherVO.getShopName() != null)
            filters.add(String.format("shopName == '%s'", voucherVO.getShopName()));
        if(voucherVO.getType()!= null)
            filters.add("type == " + voucherVO.getType());
        if(voucherVO.getTypeId() != null)
            filters.add("typeId == " + voucherVO.getTypeId());
        if(voucherVO.getTitle() != null)
            filters.add(String.format("title == '%s'", voucherVO.getTitle()));
        return filters.isEmpty() ? "" : String.join(" && ", filters);
    }

    /**
     * 将Document列表转为VoucherVO列表
     */
    private List<VoucherVO> convertDocumentsToVoucherVO(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToVoucherVO)
                .filter(Objects::nonNull)  // 过滤掉转换失败的
                .toList();
    }

    /**
     * 将单个Document转为VoucherVO
     */
    private VoucherVO convertDocumentToVoucherVO(Document document) {
        try {
            Map<String, Object> metadata = document.getMetadata();

            VoucherVO voucher = new VoucherVO();
            // 从元数据中提取字段
            if (metadata.containsKey("id")) {
                voucher.setId(Long.valueOf(metadata.get("id").toString()));
            }
            if (metadata.containsKey("shopId")) {
                voucher.setShopId(Long.valueOf(metadata.get("shopId").toString()));
            }
            if (metadata.containsKey("shopName")) {
                voucher.setShopName(metadata.get("shopName").toString());
            }
            if (metadata.containsKey("title")) {
                voucher.setTitle(metadata.get("title").toString());
            }
            if (metadata.containsKey("subTitle")) {
                voucher.setSubTitle(metadata.get("subTitle").toString());
            }
            if (metadata.containsKey("rules")) {
                voucher.setRules(metadata.get("rules").toString());
            }
            if (metadata.containsKey("payValue")) {
                voucher.setPayValue(metadata.get("payValue").toString());
            }
            if (metadata.containsKey("actualValue")) {
                voucher.setActualValue(Long.valueOf(metadata.get("actualValue").toString()));
            }
            if (metadata.containsKey("type")) {
                voucher.setType(Integer.valueOf(metadata.get("type").toString()));
            }
            if (metadata.containsKey("status")) {
                voucher.setStatus(Integer.valueOf(metadata.get("status").toString()));
            }
            if (metadata.containsKey("stock")) {
                voucher.setStock(Integer.valueOf(metadata.get("stock").toString()));
            }
            if (metadata.containsKey("beginTime")) {
                voucher.setBeginTime(LocalDateTime.parse(metadata.get("beginTime").toString()));
            }
            if (metadata.containsKey("endTime")) {
                voucher.setEndTime(LocalDateTime.parse(metadata.get("endTime").toString()));
            }

            return voucher;
        } catch (Exception e) {
            log.warn("转换Document到VoucherVO失败: {}", e.getMessage());
            return null;
        }
    }
}
