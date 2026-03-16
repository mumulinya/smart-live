package com.smartLive.search.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.ResourceTypeConstants;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ProductDoc;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helper methods for Elasticsearch document conversion.
 */
public class EsTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EsTool() {
    }

    /**
     * Convert a document object to the JSON map expected by Elasticsearch.
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
            jsonMap.put("icon", blog.getIcon());
            jsonMap.put("name", blog.getName());
            jsonMap.put("actionType", blog.getActionType());
            jsonMap.put("sourceType", blog.getSourceType());
            jsonMap.put("sourceId", blog.getSourceId());
            if (blog.getCreateTime() != null) {
                jsonMap.put("createTime", blog.getCreateTime().getTime());
            }
            return jsonMap;
        }

        if (data instanceof ShopDoc) {
            ShopDoc shop = (ShopDoc) data;
            jsonMap.put("id", shop.getId());
            jsonMap.put("name", shop.getName());
            jsonMap.put("typeId", shop.getTypeId());
            jsonMap.put("images", shop.getImages());
            jsonMap.put("shopLogo", shop.getShopLogo());
            jsonMap.put("area", shop.getArea());
            jsonMap.put("address", shop.getAddress());
            jsonMap.put("x", shop.getX());
            jsonMap.put("y", shop.getY());
            jsonMap.put("avgPrice", shop.getAvgPrice());
            jsonMap.put("sold", shop.getSold());
            jsonMap.put("comments", shop.getComments());
            jsonMap.put("score", shop.getScore());
            jsonMap.put("openHours", shop.getOpenHours());
            jsonMap.put("distance", shop.getDistance());
            jsonMap.put("actionType", shop.getActionType());
            jsonMap.put("sourceType", shop.getSourceType());
            jsonMap.put("sourceId", shop.getSourceId());
            if (shop.getX() != null && shop.getY() != null) {
                Map<String, Double> location = new HashMap<>();
                location.put("lat", shop.getY());
                location.put("lon", shop.getX());
                jsonMap.put("location", location);
            }
            if (shop.getCreateTime() != null) {
                jsonMap.put("createTime", shop.getCreateTime().getTime());
            }
            return jsonMap;
        }

        if (data instanceof UserDoc) {
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
            return jsonMap;
        }

        if (data instanceof ProductDoc) {
            ProductDoc product = (ProductDoc) data;
            jsonMap.put("id", product.getId());
            if (product.getShopId() != null && !product.getShopId().isEmpty()) {
                jsonMap.put("shopId", product.getShopId().split(",")[0]);
            }
            jsonMap.put("name", product.getName());
            jsonMap.put("subTitle", product.getSubTitle());
            jsonMap.put("category", product.getCategory());
            jsonMap.put("activityType", product.getActivityType());
            jsonMap.put("rulesJson", product.getRulesJson());
            jsonMap.put("price", product.getPrice());
            jsonMap.put("originalPrice", product.getOriginalPrice());
            jsonMap.put("stock", product.getStock());
            jsonMap.put("sold", product.getSold());
            jsonMap.put("reviews", product.getReviews());
            jsonMap.put("fans", product.getFans());
            jsonMap.put("stars", product.getStars());
            jsonMap.put("status", product.getStatus());
            jsonMap.put("validityType", product.getValidityType());
            jsonMap.put("validDays", product.getValidDays());
            jsonMap.put("coverImg", product.getCoverImg());
            jsonMap.put("shopName", product.getShopName());
            jsonMap.put("typeId", product.getTypeId());
            jsonMap.put("actionType", product.getActionType());
            jsonMap.put("sourceType", product.getSourceType());
            jsonMap.put("sourceId", product.getSourceId());
            if (product.getUseStartTime() != null) {
                jsonMap.put("useStartTime", product.getUseStartTime().getTime());
            }
            if (product.getUseEndTime() != null) {
                jsonMap.put("useEndTime", product.getUseEndTime().getTime());
            }
            if (product.getBeginTime() != null) {
                jsonMap.put("beginTime", product.getBeginTime().getTime());
            }
            if (product.getEndTime() != null) {
                jsonMap.put("endTime", product.getEndTime().getTime());
            }
            if (product.getCreateTime() != null) {
                jsonMap.put("createTime", product.getCreateTime().getTime());
            }
            return jsonMap;
        }

        return jsonMap;
    }

    public static String[] getDefaultSearchFields(Integer type) {
        switch (type) {
            case ResourceTypeConstants.BLOG_CODE:
                return new String[]{"title", "content", "name"};
            case ResourceTypeConstants.SHOP_CODE:
                return new String[]{"name", "area", "address"};
            case ResourceTypeConstants.USER_CODE:
                return new String[]{"nickName", "introduce", "city", "id"};
            case ResourceTypeConstants.PRODUCT_CODE:
                return new String[]{"name", "subTitle", "shopName"};
            default:
                return new String[]{};
        }
    }

    public static List<? extends Object> convertSearchResult(Integer type, SearchResponse response) throws Exception {
        switch (type) {
            case ResourceTypeConstants.BLOG_CODE:
                return ResponseConverter.convertToBlogList(response);
            case ResourceTypeConstants.SHOP_CODE:
                return ResponseConverter.convertToShopList(response, null);
            case ResourceTypeConstants.USER_CODE:
                return ResponseConverter.convertToUserList(response);
            case ResourceTypeConstants.PRODUCT_CODE:
                return ResponseConverter.convertToProductList(response);
            default:
                List<Map<String, Object>> result = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    result.add(hit.getSourceAsMap());
                }
                return result;
        }
    }

    public static String[] getDefaultSearchFields(String indexName) {
        switch (indexName) {
            case EsIndexNameConstants.BLOG_INDEX_NAME:
                return new String[]{"title", "content", "name"};
            case EsIndexNameConstants.SHOP_INDEX_NAME:
                return new String[]{"name", "area", "address"};
            case EsIndexNameConstants.USER_INDEX_NAME:
                return new String[]{"nickName", "introduce", "id"};
            case EsIndexNameConstants.PRODUCT_INDEX_NAME:
                return new String[]{"name", "subTitle", "shopName"};
            default:
                return new String[]{};
        }
    }

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
                List<Map<String, Object>> result = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    result.add(hit.getSourceAsMap());
                }
                return result;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> convertList(List<Object> dataList, Class<T> targetClass) {
        if (dataList == null) {
            return new ArrayList<>();
        }
        return dataList.stream()
                .map(item -> {
                    try {
                        if (item instanceof LinkedHashMap) {
                            return convertToObject((LinkedHashMap<String, Object>) item, targetClass);
                        }
                        if (targetClass.isInstance(item)) {
                            return (T) item;
                        }
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static <T> T convertToObject(Map<String, Object> map, Class<T> targetClass) {
        try {
            return OBJECT_MAPPER.convertValue(map, targetClass);
        } catch (Exception e) {
            return null;
        }
    }
}
