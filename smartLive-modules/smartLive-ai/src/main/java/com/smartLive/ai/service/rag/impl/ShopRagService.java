package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.utils.RagMetadataValueUtils;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 店铺 RAG 服务实现类。
 */
@Service
@Slf4j
public class ShopRagService implements IShopRagService {

    private final VectorStore shopVectorStore;
    private final RemoteShopService remoteShopService;

    /**
     * 构造店铺 RAG 服务实现类。
     */
    public ShopRagService(@Qualifier("shopVectorStore") VectorStore vectorStore,
                          RemoteShopService remoteShopService) {
        this.shopVectorStore = vectorStore;
        this.remoteShopService = remoteShopService;
    }

    /**
     * 获取店铺列表。
     */
    @Override
    public List<ShopVO> getShopList(ShopVO shopVo, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank()) ? "food restaurant" : userMessage;

        List<Document> results = shopVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ragQuery)
                        .topK(10)
                        .build());

        log.info("Shop RAG search results: {}", results);
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<ShopVO> shopVOS = convertDocumentsToShopVO(results);
        boolean hasUserCoordinate = shopVo != null && hasValidCoordinatePair(shopVo.getX(), shopVo.getY());
        shopVOS.forEach(shop -> applyDistance(shop, hasUserCoordinate ? shopVo.getX() : null, hasUserCoordinate ? shopVo.getY() : null));

        log.info("Shop RAG search returned {} records", shopVOS.size());
        return shopVOS;
    }

    /**
     * 获取店铺详情。
     */
    @Override
    public ShopVO getShopDetails(ShopVO shopVo, String userMessage) {
        if (shopVo == null) {
            return null;
        }

        if (shopVo.getId() != null) {
            ShopDTO shopDTO = remoteShopService.getShopById(shopVo.getId());
            return applyDistance(convertShopDtoToShopVO(shopDTO), shopVo.getX(), shopVo.getY());
        }

        if (StringUtils.hasText(shopVo.getName())) {
            ShopDTO shopDTO = remoteShopService.getShopByShopName(shopVo.getName().trim());
            if (shopDTO != null) {
                return applyDistance(convertShopDtoToShopVO(shopDTO), shopVo.getX(), shopVo.getY());
            }
        }

        String query = StringUtils.hasText(userMessage) ? userMessage : "shop details";
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(1);

        String filter = buildFilterExpression(shopVo);
        if (StringUtils.hasText(filter)) {
            builder.filterExpression(filter);
        }

        List<Document> results = shopVectorStore.similaritySearch(builder.build());
        log.info("Shop detail fallback search results: {}", results);
        if (results == null || results.isEmpty()) {
            return null;
        }

        List<ShopVO> shops = convertDocumentsToShopVO(results);
        if (shops.isEmpty()) {
            log.warn("Convert shop detail fallback result failed");
            return null;
        }
        return applyDistance(shops.get(0), shopVo.getX(), shopVo.getY());
    }

    /**
     * 转换店铺数据传输对象店铺。
     */
    private ShopVO convertShopDtoToShopVO(ShopDTO shopDTO) {
        if (shopDTO == null) {
            return null;
        }
        ShopVO shopVO = new ShopVO();
        shopVO.setId(shopDTO.getId());
        shopVO.setName(shopDTO.getName());
        shopVO.setTypeId(shopDTO.getTypeId());
        shopVO.setImages(shopDTO.getImages());
        shopVO.setShopLogo(shopDTO.getShopLogo());
        shopVO.setArea(shopDTO.getArea());
        shopVO.setAddress(shopDTO.getAddress());
        shopVO.setX(shopDTO.getX());
        shopVO.setY(shopDTO.getY());
        shopVO.setAvgPrice(shopDTO.getAvgPrice());
        shopVO.setSold(shopDTO.getSold());
        shopVO.setComments(shopDTO.getReviews());
        shopVO.setScore(shopDTO.getScore() == null ? 0 : shopDTO.getScore() / 10.0);
        shopVO.setOpenHours(shopDTO.getOpenHours());
        return shopVO;
    }

    /**
     * 应用距离信息。
     */
    private ShopVO applyDistance(ShopVO shopVO, Double userX, Double userY) {
        if (shopVO == null) {
            return null;
        }
        if (!hasValidCoordinatePair(userX, userY)) {
            shopVO.setDistance(null);
            shopVO.setDistanceText(null);
            return shopVO;
        }
        double distance = getDistance(userX, userY, shopVO.getX(), shopVO.getY());
        shopVO.setDistance(distance);
        if (distance >= 0) {
            if (distance < 1000) {
                shopVO.setDistanceText(String.format("%.0fm", distance));
            } else {
                shopVO.setDistanceText(String.format("%.1fkm", distance / 1000));
            }
        } else {
            shopVO.setDistanceText("unknown");
        }
        return shopVO;
    }

    /**
     * 获取距离值。
     */
    private Double getDistance(Double x, Double y, Double shopVoX, Double shopVoY) {
        if (x == null || y == null || shopVoX == null || shopVoY == null) {
            return -1.0;
        }
        if (!isValidCoordinate(x, y) || !isValidCoordinate(shopVoX, shopVoY)) {
            return -1.0;
        }
        if (x.equals(shopVoX) && y.equals(shopVoY)) {
            return 0.0;
        }

        final double earthRadius = 6371393;
        double radLat1 = Math.toRadians(y);
        double radLat2 = Math.toRadians(shopVoY);
        double a = Math.toRadians(y - shopVoY);
        double b = Math.toRadians(x - shopVoX);

        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2)
                        + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)
        ));
        double distance = s * earthRadius;
        return Math.round(distance * 100.0) / 100.0;
    }

    /**
     * 判断坐标是否有效。
     */
    private boolean isValidCoordinate(Double lng, Double lat) {
        return lng != null && lat != null
                && lng >= -180 && lng <= 180
                && lat >= -90 && lat <= 90;
    }

    /**
     * 判断坐标对是否有效。
     */
    private boolean hasValidCoordinatePair(Double x, Double y) {
        return x != null && y != null && isValidCoordinate(x, y);
    }

    /**
     * 构建过滤表达式。
     */
    private String buildFilterExpression(ShopVO shopVo) {
        List<String> filters = new ArrayList<>();
        if (shopVo.getId() != null) {
            filters.add("id == " + shopVo.getId());
        }
        if (shopVo.getTypeId() != null) {
            filters.add("typeId == " + shopVo.getTypeId());
        }
        if (StringUtils.hasText(shopVo.getName())) {
            filters.add(String.format("name == '%s'", shopVo.getName().trim().replace("'", "\\'")));
        }
        if (shopVo.getAvgPrice() != null && shopVo.getAvgPrice() > 0) {
            filters.add("avgPrice >= " + (shopVo.getAvgPrice() - 20) + " && avgPrice <= " + (shopVo.getAvgPrice() + 20));
        }
        if (StringUtils.hasText(shopVo.getArea())) {
            filters.add(String.format("address.contains('%s')", shopVo.getArea().trim().replace("'", "\\'")));
        }
        return filters.isEmpty() ? "" : String.join(" && ", filters);
    }

    /**
     * 将文档列表转换为店铺对象。
     */
    private List<ShopVO> convertDocumentsToShopVO(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToShopVO)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 将文档转换为店铺对象。
     */
    private ShopVO convertDocumentToShopVO(Document document) {
        try {
            Map<String, Object> metadata = document.getMetadata();

            ShopVO shop = new ShopVO();
            shop.setId(RagMetadataValueUtils.toLong(metadata.get("id")));
            shop.setName(RagMetadataValueUtils.toStringValue(metadata.get("name")));
            shop.setTypeId(RagMetadataValueUtils.toLong(metadata.get("typeId")));
            shop.setArea(RagMetadataValueUtils.toStringValue(metadata.get("area")));
            shop.setAddress(RagMetadataValueUtils.toStringValue(metadata.get("address")));
            shop.setAvgPrice(RagMetadataValueUtils.toInteger(metadata.get("avgPrice")));
            shop.setScore(RagMetadataValueUtils.toInteger(metadata.get("score")) / 10.0);
            shop.setSold(RagMetadataValueUtils.toInteger(metadata.get("sold")));
            shop.setComments(RagMetadataValueUtils.toInteger(metadata.get("comments")));
            shop.setX(RagMetadataValueUtils.toDouble(metadata.get("x")));
            shop.setY(RagMetadataValueUtils.toDouble(metadata.get("y")));
            shop.setOpenHours(RagMetadataValueUtils.toStringValue(metadata.get("openHours")));
            shop.setShopLogo(RagMetadataValueUtils.toStringValue(metadata.get("shopLogo")));
            shop.setImages(RagMetadataValueUtils.toStringValue(metadata.get("images")));
            return shop;
        } catch (Exception e) {
            log.warn("Failed to convert document to ShopVO: {}", e.getMessage());
            return null;
        }
    }
}