package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.utils.RagMetadataValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ShopRagService implements IShopRagService {

    private final VectorStore shopVectorStore;

    public ShopRagService(@Qualifier("shopVectorStore") VectorStore vectorStore) {
        this.shopVectorStore = vectorStore;
    }

    @Override
    public List<ShopVO> getShopList(ShopVO shopVo, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank()) ? "food restaurant" : userMessage;

        List<Document> results = this.shopVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ragQuery)
                        .topK(10)
                        // .filterExpression(buildFilterExpression(shopVo))
                        .build());

        log.info("RAG search results: {}", results);
        if (results.isEmpty()) {
            return List.of();
        }

        List<ShopVO> shopVOS = convertDocumentsToShopVO(results);
        boolean hasUserCoordinate = hasValidCoordinatePair(shopVo.getX(), shopVo.getY());
        shopVOS.forEach(shopVO -> {
            if (!hasUserCoordinate) {
                shopVO.setDistance(null);
                shopVO.setDistanceText(null);
                return;
            }
            double distance = getDistance(shopVO.getX(), shopVO.getY(), shopVo.getX(), shopVo.getY());
            if (distance >= 0) {
                if (distance < 1000) {
                    shopVO.setDistanceText(String.format("%.0fm", distance));
                } else {
                    shopVO.setDistanceText(String.format("%.1fkm", distance / 1000));
                }
            } else {
                shopVO.setDistanceText("unknown");
            }
            shopVO.setDistance(distance);
        });

        log.info("RAG search returned {} shops", shopVOS.size());
        return shopVOS;
    }

    @Override
    public ShopVO getShopDetails(ShopVO shopVo, String userMessage) {
        List<Document> results = shopVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)
                        .topK(1)
                        .filterExpression(buildFilterExpression(shopVo))
                        .build());

        log.info("RAG detail search results: {}", results);
        if (results.isEmpty()) {
            return null;
        }

        List<ShopVO> vo = convertDocumentsToShopVO(results);
        if (vo.isEmpty()) {
            log.warn("RAG detail search got documents but all failed to convert");
            return null;
        }
        boolean hasUserCoordinate = hasValidCoordinatePair(shopVo.getX(), shopVo.getY());
        vo.forEach(item -> {
            if (!hasUserCoordinate) {
                item.setDistance(null);
                item.setDistanceText(null);
                return;
            }
            double distance = getDistance(shopVo.getX(), shopVo.getY(), item.getX(), item.getY());
            item.setDistance(distance);
            if (distance >= 0) {
                if (distance < 1000) {
                    item.setDistanceText(String.format("%.0fm", distance));
                } else {
                    item.setDistanceText(String.format("%.1fkm", distance / 1000));
                }
            } else {
                item.setDistanceText("unknown");
            }
        });

        log.info("RAG detail search returned {} shops", vo.size());
        return vo.get(0);
    }

    private Double getDistance(Double x, Double y, Double shopVoX, Double shopVoY) {
        log.info("Calculating distance | x={}, y={}, shopVoX={}, shopVoY={}", x, y, shopVoX, shopVoY);
        if (x == null || y == null || shopVoX == null || shopVoY == null) {
            log.warn("Invalid coordinates. x/y or shop coordinates are null");
            return -1.0;
        }

        if (!isValidCoordinate(x, y) || !isValidCoordinate(shopVoX, shopVoY)) {
            log.warn("Invalid coordinate range");
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

    private boolean isValidCoordinate(Double lng, Double lat) {
        return lng != null && lat != null
                && lng >= -180 && lng <= 180
                && lat >= -90 && lat <= 90;
    }

    private boolean hasValidCoordinatePair(Double x, Double y) {
        return x != null && y != null && isValidCoordinate(x, y);
    }

    private String buildFilterExpression(ShopVO shopVo) {
        List<String> filters = new ArrayList<>();
        if (shopVo.getId() != null) {
            filters.add("id == " + shopVo.getId());
        }
        if (shopVo.getTypeId() != null) {
            filters.add("typeId == " + shopVo.getTypeId());
        }
        if (shopVo.getName() != null) {
            filters.add(String.format("name == '%s'", shopVo.getName()));
        }
        if (shopVo.getAvgPrice() != null && shopVo.getAvgPrice() > 0) {
            filters.add("avgPrice >= " + (shopVo.getAvgPrice() - 20) + " && avgPrice <= " + (shopVo.getAvgPrice() + 20));
        }
        if (shopVo.getArea() != null && !shopVo.getArea().trim().isEmpty()) {
            filters.add(String.format("address.contains('%s')", shopVo.getArea().trim()));
        }
        return filters.isEmpty() ? "" : String.join(" && ", filters);
    }

    private List<ShopVO> convertDocumentsToShopVO(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToShopVO)
                .filter(Objects::nonNull)
                .toList();
    }

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
            return shop;
        } catch (Exception e) {
            log.warn("Failed to convert Document to ShopVO: {}", e.getMessage());
            return null;
        }
    }

}

