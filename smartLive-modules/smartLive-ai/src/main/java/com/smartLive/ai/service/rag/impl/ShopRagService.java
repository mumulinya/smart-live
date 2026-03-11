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

/**
 * 店铺 RAG (检索增强生成) 服务
 * 在 Milvus 中检索店铺信息，并根据经纬度计算用户与店铺的距离
 *
 * @author smartLive
 */
@Service
@Slf4j
public class ShopRagService implements IShopRagService {

    private final VectorStore shopVectorStore;

    public ShopRagService(@Qualifier("shopVectorStore") VectorStore vectorStore) {
        this.shopVectorStore = vectorStore;
    }

    /**
     * 检索店铺列表
     * 支持语义检索、分页推荐，并自动计算距离文本
     *
     * @param shopVo 搜索参数（包含用户坐标）
     * @param userMessage 用户输入的搜索关键词
     * @return 包含距离信息的店铺 VO 列表
     */
    @Override
    public List<ShopVO> getShopList(ShopVO shopVo, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank()) ? "food restaurant" : userMessage;

        List<Document> results = this.shopVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ragQuery)
                        .topK(10)
                        // .filterExpression(buildFilterExpression(shopVo))
                        .build());

        log.info("店铺 RAG 检索结果: {}", results);
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
            // 计算用户位置到店铺位置的距离
            double distance = getDistance(shopVo.getX(), shopVo.getY(), shopVO.getX(), shopVO.getY());
            if (distance >= 0) {
                if (distance < 1000) {
                    shopVO.setDistanceText(String.format("%.0fm", distance));
                } else {
                    shopVO.setDistanceText(String.format("%.1fkm", distance / 1000));
                }
            } else {
                shopVO.setDistanceText("位置未知");
            }
            shopVO.setDistance(distance);
        });

        log.info("店铺 RAG 检索返回 {} 条记录", shopVOS.size());
        return shopVOS;
    }

    /**
     * 获取店铺详情（精准检索）
     */
    @Override
    public ShopVO getShopDetails(ShopVO shopVo, String userMessage) {
        List<Document> results = shopVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)
                        .topK(1)
                        .filterExpression(buildFilterExpression(shopVo))
                        .build());

        log.info("店铺 RAG 详情检索结果: {}", results);
        if (results.isEmpty()) {
            return null;
        }

        List<ShopVO> vo = convertDocumentsToShopVO(results);
        if (vo.isEmpty()) {
            log.warn("店铺 RAG 详情项转换失败");
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
                item.setDistanceText("未知");
            }
        });

        return vo.get(0);
    }

    /**
     * 计算地球上两点间的距离（哈弗辛公式）
     *
     * @param x 用户经度
     * @param y 用户纬度
     * @param shopVoX 店铺经度
     * @param shopVoY 店铺纬度
     * @return 距离（单位：米）
     */
    private Double getDistance(Double x, Double y, Double shopVoX, Double shopVoY) {
        log.info("开始计算距离 | 用户: {}, {} | 店铺: {}, {}", x, y, shopVoX, shopVoY);
        if (x == null || y == null || shopVoX == null || shopVoY == null) {
            log.warn("经纬度参数不全");
            return -1.0;
        }

        if (!isValidCoordinate(x, y) || !isValidCoordinate(shopVoX, shopVoY)) {
            log.warn("经纬度数值超出有效范围");
            return -1.0;
        }

        if (x.equals(shopVoX) && y.equals(shopVoY)) {
            return 0.0;
        }

        final double earthRadius = 6371393; // 地球半径，单位米

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

