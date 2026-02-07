package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.rag.IShopRagService;
import lombok.RequiredArgsConstructor;
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
    /**
     * 获取店铺列表
     *
     * @param shopVo
     * @param userMessage
     * @return
     */
    @Override
    public List<ShopVO> getShopList(ShopVO shopVo, String userMessage) {
        // 使用用户原始消息作为RAG查询
        String ragQuery = userMessage != null ? userMessage : "餐饮 美食";

        // 调用RAG - 使用用户原始消息
        List<Document> results = this.shopVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ragQuery)  // 使用用户消息
                        .topK(10)
                        .filterExpression(buildFilterExpression(shopVo))
                        .build());
        log.info("🔍 RAG搜索结果：{}", results);
        if (results.isEmpty()){
            return null;
        }
        // 2. 将Document列表转为ShopVO列表
        List<ShopVO> shopVOS = convertDocumentsToShopVO(results);
        //计算距离店铺多少 米
        shopVOS.forEach(shopVO -> {
            double distance = getDistance(shopVO.getX(), shopVO.getY(), shopVo.getX(), shopVo.getY());
            // 格式化显示文本
            if (distance >= 0) {
                if (distance < 1000) {
                    shopVO.setDistanceText(String.format("%.0fm", distance));
                } else {
                    shopVO.setDistanceText(String.format("%.1fkm", distance / 1000));
                }
            } else {
                shopVO.setDistanceText("未知");
            }
            shopVO.setDistance(distance);
        });
        log.info("🔍 RAG搜索到 {} 个店铺", shopVOS);
        return shopVOS;
    }

    /**
     * 获取店铺详情
     *
     * @param shopVo
     * @param userMessage
     * @return
     */
    @Override
    public ShopVO getShopDetails(ShopVO shopVo, String userMessage) {


        List<Document> results = shopVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)  // 使用用户消息
                        .topK(1)
                        .filterExpression(buildFilterExpression(shopVo))
                        .build());
        log.info("🔍 RAG搜索结果：{}", results);
        if (results.isEmpty()){
            return null;
        }
        // 2. 将Document列表转为ShopVO列表
        List<ShopVO> vo = convertDocumentsToShopVO(results);

        //计算距离店铺多少 米
        vo.forEach(shopVO -> {
            double distance = getDistance(shopVo.getX(), shopVo.getY(),shopVO.getX(), shopVO.getY());
            shopVO.setDistance(distance);

            // 格式化显示文本
            if (distance >= 0) {
                if (distance < 1000) {
                    shopVO.setDistanceText(String.format("%.0fm", distance));
                } else {
                    shopVO.setDistanceText(String.format("%.1fkm", distance / 1000));
                }
            } else {
                shopVO.setDistanceText("未知");
            }
        });
        log.info("🔍 RAG搜索到 {} 个店铺", vo);
        return vo.get(0);
    }

    /**
     * 计算两个坐标点之间的距离（单位：米）
     * 使用 Haversine 公式
     * @param x 经度1
     * @param y 纬度1
     * @param shopVoX 经度2
     * @param shopVoY 纬度2
     * @return 距离（米），如果坐标无效返回 -1
     */
    private Double getDistance(Double x, Double y, Double shopVoX, Double shopVoY) {
        log.info("🔍 计算两个坐标点之间的距离 | x={}, y={}, shopVoX={}, shopVoY={}", x, y, shopVoX, shopVoY);
        // 参数校验
        if (x == null || y == null || shopVoX == null || shopVoY == null) {
            log.warn("参数无效，请检查输入的参数");
            return -1.0;
        }

        // 坐标范围校验
        if (!isValidCoordinate(x, y) || !isValidCoordinate(shopVoX, shopVoY)) {
            log.warn("坐标无效，请检查输入的坐标");
            return -1.0;
        }

        // 如果是同一个点，直接返回0
        if (x.equals(shopVoX) && y.equals(shopVoY)) {
            return 0.0;
        }

        final double EARTH_RADIUS = 6371393; // 地球半径（米）

        double radLat1 = Math.toRadians(y);
        double radLat2 = Math.toRadians(shopVoY);
        double a = Math.toRadians(y - shopVoY);
        double b = Math.toRadians(x - shopVoX);

        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2) +
                        Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)
        ));

        double distance = s * EARTH_RADIUS;
        return Math.round(distance * 100.0) / 100.0;
    }

    /**
     * 校验坐标是否有效
     */
    private boolean isValidCoordinate(Double lng, Double lat) {
        return lng != null && lat != null &&
                lng >= -180 && lng <= 180 &&
                lat >= -90 && lat <= 90;
    }

    /**
     * 构建过滤条件
     */
    private String buildFilterExpression(ShopVO shopVo) {
        List<String> filters = new ArrayList<>();
        //店铺分类
        if (shopVo.getTypeId() != null) {
            filters.add("typeId == " + shopVo.getTypeId());
        }
        if (shopVo.getName() != null) {
            //中文要这样写
            filters.add(String.format("name == '%s'", shopVo.getName()));
        }
        if (shopVo.getAvgPrice() != null&& shopVo.getAvgPrice() > 0) {
            // 根据价格区间构建过滤条件
            filters.add("avgPrice >= " + (shopVo.getAvgPrice() - 20) + " && avgPrice <= " + (shopVo.getAvgPrice() + 20));
        }
        // 区域模糊查询
        if (shopVo.getDistrict() != null && !shopVo.getDistrict().trim().isEmpty()) {
            // 使用 contains 进行模糊查询
            filters.add(String.format("address.contains('%s')", shopVo.getDistrict().trim()));
        }
        // 地址模糊查询
        if (shopVo.getAddress() != null && !shopVo.getAddress().trim().isEmpty()) {
            // 使用 contains 进行模糊查询
            filters.add(String.format("address.contains('%s')", shopVo.getAddress().trim()));
        }
        return filters.isEmpty() ? "" : String.join(" && ", filters);
    }

    /**
     * 将Document列表转为ShopVO列表
     */
    private List<ShopVO> convertDocumentsToShopVO(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToShopVO)
                .filter(Objects::nonNull)  // 过滤掉转换失败的
                .toList();
    }

    /**
     * 将单个Document转为ShopVO
     */
    private ShopVO convertDocumentToShopVO(Document document) {
        try {
            Map<String, Object> metadata = document.getMetadata();

            ShopVO shop = new ShopVO();
            // 从元数据中提取字段
            if (metadata.containsKey("id")) {
                shop.setId(Long.valueOf(metadata.get("id").toString()));
            }
            if (metadata.containsKey("name")) {
                shop.setName(metadata.get("name").toString());
            }
            if (metadata.containsKey("typeId")) {
                shop.setTypeId(Long.valueOf((metadata.get("typeId").toString())));
            }
            if (metadata.containsKey("area")) {
                shop.setArea(metadata.get("area").toString());
            }
            if (metadata.containsKey("address")) {
                shop.setAddress(metadata.get("address").toString());
            }
            if (metadata.containsKey("avgPrice")) {
                shop.setAvgPrice(Integer.valueOf(metadata.get("avgPrice").toString()));
            }
            if (metadata.containsKey("score")) {
                shop.setScore(Integer.valueOf(metadata.get("score").toString()));
            }
            if (metadata.containsKey("sold")) {
                shop.setSold(Integer.valueOf(metadata.get("sold").toString()));
            }
            if (metadata.containsKey("x")) {
                shop.setX(Double.valueOf(metadata.get("x").toString()));
            }
            if (metadata.containsKey("y")) {
                shop.setY(Double.valueOf(metadata.get("y").toString()));
            }



            return shop;
        } catch (Exception e) {
            log.warn("转换Document到ShopVO失败: {}", e.getMessage());
            return null;
        }
    }
}
