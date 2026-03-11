package com.smartLive.search.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.ResourceTypeConstants;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.ProductDoc;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ES 搜索引擎核心工具类
 * 提供 Java 对象与 ES JSON Map 之间的双向转换，以及基于索引类型的搜索字段动态获取。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
public class EsTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private EsTool() {}

    /**
     * 将业务领域对象 (BlogDoc, ShopDoc, etc.) 适配为 ES 索引要求的 JSON Map。
     * 包含对地理位置坐标 (location) 和时间戳 (TimeMillis) 的特殊转换。
     * 
     * @param data 原始实体对象
     * @return 符合 ES Mapping 的 Map 格式
     */
   public static Map<String, Object> convertToJsonMap(Object data) {
        Map<String, Object> jsonMap = new HashMap<>();

        if (data instanceof BlogDoc) {
            BlogDoc blog = (BlogDoc) data;
            jsonMap.put("id", blog.getId());
            jsonMap.put("typeId", blog.getTypeId());
            jsonMap.put("title", blog.getTitle());
            jsonMap.put("images", blog.getImages());
            jsonMap.put("content", blog.getContent());
            jsonMap.put("liked", blog.getLiked());
            if (blog.getCreateTime() != null) {
                jsonMap.put("createTime", blog.getCreateTime().getTime());
            }
            jsonMap.put("icon", blog.getIcon());
            jsonMap.put("name", blog.getName());

        } else if (data instanceof ShopDoc) {
            ShopDoc shop = (ShopDoc) data;
            jsonMap.put("id", shop.getId());
            jsonMap.put("name", shop.getName());
            jsonMap.put("typeId", shop.getTypeId());
            jsonMap.put("shopLogo", shop.getShopLogo());
            jsonMap.put("area", shop.getArea());
            jsonMap.put("address", shop.getAddress());
            // LBS 特殊映射：将 x/y 坐标包装为 ES 的 geo_point 对象
            if (shop.getX() != null && shop.getY() != null) {
                Map<String, Double> location = new HashMap<>();
                location.put("lat", shop.getY());
                location.put("lon", shop.getX());
                jsonMap.put("location", location);
            }
            jsonMap.put("avgPrice", shop.getAvgPrice());
            jsonMap.put("sold", shop.getSold());
            jsonMap.put("comments", shop.getComments());
            jsonMap.put("score", shop.getScore());
            jsonMap.put("openHours", shop.getOpenHours());
            if (shop.getCreateTime() != null) {
                jsonMap.put("createTime", shop.getCreateTime().getTime());
            }

        } else if (data instanceof UserDoc) {
            UserDoc user = (UserDoc) data;
            jsonMap.put("id", user.getId());
            jsonMap.put("nickName", user.getNickName());
            jsonMap.put("icon", user.getIcon());
            jsonMap.put("isFollow", user.getIsFollow());
            jsonMap.put("introduce", user.getIntroduce());
            jsonMap.put("city", user.getCity());
            if (user.getCreateTime() != null) {
                jsonMap.put("createTime", user.getCreateTime().getTime());
            }

        } else if (data instanceof ProductDoc) {
            ProductDoc product = (ProductDoc) data;
            jsonMap.put("id", product.getId());
            if (product.getShopId() != null && !product.getShopId().isEmpty()) {
                jsonMap.put("shopId", Long.valueOf(product.getShopId().split(",")[0]));
            }
            jsonMap.put("name", product.getName());
            jsonMap.put("subTitle", product.getSubTitle());
            jsonMap.put("rulesJson", product.getRulesJson());
            jsonMap.put("price", product.getPrice());
            jsonMap.put("originalPrice", product.getOriginalPrice());
            jsonMap.put("activityType", product.getActivityType());
            jsonMap.put("status", product.getStatus());
            jsonMap.put("stock", product.getStock());
            if (product.getBeginTime() != null) {
                jsonMap.put("beginTime", product.getBeginTime().getTime());
            }
            if (product.getEndTime() != null) {
                jsonMap.put("endTime", product.getEndTime().getTime());
            }
            jsonMap.put("shopName", product.getShopName());
            jsonMap.put("typeId", product.getTypeId());
            if (product.getCreateTime() != null) {
                jsonMap.put("createTime", product.getCreateTime().getTime());
            }
        }
        return jsonMap;
    }

    /**
     * 根据业务类型代码获取需要参与搜索匹配的字段列表
     */
    public static String[] getDefaultSearchFields(Integer type) {
        switch (type) {
            case ResourceTypeConstants.BLOG_CODE: return new String[]{"title", "content", "name"};
            case ResourceTypeConstants.SHOP_CODE: return new String[]{"name", "area", "address"};
            case ResourceTypeConstants.USER_CODE: return new String[]{"nickName", "introduce", "city","id"};
            case ResourceTypeConstants.PRODUCT_CODE: return new String[]{"name", "subTitle", "shopName"};
            default: return new String[]{};
        }
    }

    /**
     * 自动解析搜索响应并转换为强类型列表
     */
    public static List<? extends Object> convertSearchResult(Integer type, SearchResponse response) throws Exception {
        switch (type) {
            case ResourceTypeConstants.BLOG_CODE: return ResponseConverter.convertToBlogList(response);
            case ResourceTypeConstants.SHOP_CODE: return ResponseConverter.convertToShopList(response, null);
            case ResourceTypeConstants.USER_CODE: return ResponseConverter.convertToUserList(response);
            case ResourceTypeConstants.PRODUCT_CODE: return ResponseConverter.convertToProductList(response);
            default:
                List<Map<String, Object>> result = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    result.add(hit.getSourceAsMap());
                }
                return result;
        }
    }

    /**
     * 根据索引名称获取其默认的全文检索字段
     */
    public static String[] getDefaultSearchFields(String indexName) {
        switch (indexName) {
            case EsIndexNameConstants.BLOG_INDEX_NAME: return new String[]{"title", "content", "name"};
            case EsIndexNameConstants.SHOP_INDEX_NAME: return new String[]{"name", "area", "address"};
            case EsIndexNameConstants.USER_INDEX_NAME: return new String[]{"nickName", "introduce","id"};
            case EsIndexNameConstants.PRODUCT_INDEX_NAME: return new String[]{"name", "subTitle", "shopName"};
            default: return new String[]{};
        }
    }
    /**
     * 将搜索结果转换为对象列表
     */
    public static List<? extends Object> convertSearchResult(String indexName, SearchResponse response) throws Exception {
        switch (indexName) {
            case EsIndexNameConstants.BLOG_INDEX_NAME:
                return ResponseConverter.convertToBlogList(response);
            case EsIndexNameConstants.USER_INDEX_NAME:
                return ResponseConverter.convertToUserList(response);
            case EsIndexNameConstants.SHOP_INDEX_NAME:
                return ResponseConverter.convertToShopList(response, null);
            case EsIndexNameConstants.PRODUCT_INDEX_NAME:
                return ResponseConverter.convertToProductList(response);
            default:
                // 返回原始命中数据
                List<Map<String, Object>> result = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    result.add(hit.getSourceAsMap());
                }
                return result;
        }
    }

    /**
     * 批量转换 Map 列表到特定 Doc 类型
     * 兼容 LinkedHashMap (Jackson 默认) 和强类型实体。
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> convertList(List<Object> dataList, Class<T> targetClass) {
        if (dataList == null) return new ArrayList<>();
        return dataList.stream()
                .map(item -> {
                    try {
                        if (item instanceof LinkedHashMap) return convertToObject((LinkedHashMap<String, Object>) item, targetClass);
                        else if (targetClass.isInstance(item)) return (T) item;
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将单条 Map 数据映射为 Java Bean
     */
    public static <T> T convertToObject(Map<String, Object> map, Class<T> targetClass) {
        try {
            return objectMapper.convertValue(map, targetClass);
        } catch (Exception e) {
            return null;
        }
    }
}
