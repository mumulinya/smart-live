package com.smartLive.search.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.domain.blog.Blog;
import com.smartLive.common.core.domain.markteing.Voucher;
import com.smartLive.common.core.domain.shop.ShopDTO;
import com.smartLive.common.core.domain.user.User;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.VoucherDoc;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EsTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将数据对象转换为JSON格式的Map
     */
   public static Map<String, Object> convertToJsonMap(Object data) {
       System.out.println("进入转换格式："+data);
        Map<String, Object> jsonMap = new HashMap<>();

        if (data instanceof BlogDoc) {
            BlogDoc blog = (BlogDoc) data;
            jsonMap.put("id", blog.getId());
            jsonMap.put("shopId", blog.getShopId());
            jsonMap.put("typeId", blog.getTypeId());
            jsonMap.put("userId", blog.getUserId());
            jsonMap.put("title", blog.getTitle());
            jsonMap.put("images", blog.getImages());
            jsonMap.put("content", blog.getContent());
            jsonMap.put("liked", blog.getLiked());
            jsonMap.put("comments", blog.getComments());
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
            jsonMap.put("images", shop.getImages());
            jsonMap.put("area", shop.getArea());
            jsonMap.put("address", shop.getAddress());
            // 处理地理位置
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

        } else if (data instanceof VoucherDoc) {
            VoucherDoc voucher = (VoucherDoc) data;
            jsonMap.put("id", voucher.getId());
            jsonMap.put("shopId", voucher.getShopId());
            jsonMap.put("title", voucher.getTitle());
            jsonMap.put("subTitle", voucher.getSubTitle());
            jsonMap.put("rules", voucher.getRules());
            jsonMap.put("payValue", voucher.getPayValue());
            jsonMap.put("actualValue", voucher.getActualValue());
            jsonMap.put("type", voucher.getType());
            jsonMap.put("status", voucher.getStatus());
            jsonMap.put("stock", voucher.getStock());
            if (voucher.getBeginTime() != null) {
                jsonMap.put("beginTime", voucher.getBeginTime().getTime());
            }
            if (voucher.getEndTime() != null) {
                jsonMap.put("endTime", voucher.getEndTime().getTime());
            }
            jsonMap.put("shopName", voucher.getShopName());
            jsonMap.put("typeId", voucher.getTypeId());
            if (voucher.getCreateTime() != null) {
                jsonMap.put("createTime", voucher.getCreateTime().getTime());
            }
        }

        return jsonMap;
    }
    /**
     * 获取索引的默认搜索字段
     */

    public static String[] getDefaultSearchFields(String indexName) {
        switch (indexName) {
            case "blog_index": return new String[]{"title", "content", "name"};
            case "shop_index": return new String[]{"name", "area", "address"};
            case "user_index": return new String[]{"nickName", "introduce", "city"};
            case "voucher_index": return new String[]{"title", "subTitle", "rules", "shopName"};
            default: return new String[]{};
        }
    }
    /**
     * 将搜索结果转换为对象列表
     */
    public static List<? extends Object> convertSearchResult(String indexName, SearchResponse response) throws Exception {
        switch (indexName) {
            case "blog_index":
                return ResponseConverter.convertToBlogList(response);
            case "shop_index":
                return ResponseConverter.convertToShopList(response);
            case "user_index":
                return ResponseConverter.convertToUserList(response);
            case "voucher_index":
                return ResponseConverter.convertToVoucherList(response);
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
     * 根据索引名称获取对应的ID生成器
     */
    public static Function<Object, String> getIdGeneratorByIndex(String indexName) {
        switch (indexName) {
            case "shop_index":
                return data -> {
                    if (data instanceof ShopDTO) {
                        return ((ShopDTO) data).getId().toString();
                    } else if (data instanceof ShopDoc) {
                        return ((ShopDoc) data).getId().toString();
                    } else if (data instanceof Map) {
                        // 处理 LinkedHashMap 等 Map 类型
                        Object id = ((Map<?, ?>) data).get("id");
                        if (id != null) {
                            return id.toString();
                        } else {
                            throw new IllegalArgumentException("Map中未找到id字段");
                        }
                    }
                    throw new IllegalArgumentException("不支持的店铺数据类型: " + (data != null ? data.getClass().getName() : "null"));
                };

            case "blog_index":
                return data -> {
                    if (data instanceof Blog) {
                        return ((Blog) data).getId().toString();
                    } else if (data instanceof BlogDoc) {
                        return ((BlogDoc) data).getId().toString();
                    }
                    throw new IllegalArgumentException("不支持的博客数据类型");
                };

            case "user_index":
                return data -> {
                    if (data instanceof User) {
                        return ((User) data).getId().toString();
                    } else if (data instanceof UserDoc) {
                        return ((UserDoc) data).getId().toString();
                    }
                    throw new IllegalArgumentException("不支持的用庺数据类型");
                };

            case "voucher_index":
                return data -> {
                    if (data instanceof Voucher) {
                        return ((Voucher) data).getId().toString();
                    } else if (data instanceof VoucherDoc) {
                        return ((VoucherDoc) data).getId().toString();
                    }
                    throw new IllegalArgumentException("不支持的代金券数据类型");
                };

            default:
                return Object::toString;
        }
    }

    /**
     * 批量转换LinkedHashMap列表到指定类型
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> convertList(List<Object> dataList, Class<T> targetClass) {
        if (dataList == null) {
            return new ArrayList<>();
        }

        return dataList.stream()
                .map(item -> {
                    try {
                        if (item instanceof LinkedHashMap) {
                            // 处理Feign传输的LinkedHashMap
                            return convertToObject((LinkedHashMap<String, Object>) item, targetClass);
                        } else if (targetClass.isInstance(item)) {
                            // 如果已经是目标类型，直接返回
                            return (T) item;
                        } else {
                            System.err.println("无法转换的类型: " + item.getClass().getName() + " 到 " + targetClass.getName());
                            return null;
                        }
                    } catch (Exception e) {
                        System.err.println("转换失败: " + e.getMessage());
                        return null;
                    }
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    /**
     * 将LinkedHashMap转换为特定类型的对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertToObject(Map<String, Object> map, Class<T> targetClass) {
        try {
            return objectMapper.convertValue(map, targetClass);
        } catch (Exception e) {
            System.err.println("转换Map到" + targetClass.getSimpleName() + "失败: " + e.getMessage());
            return null;
        }
    }


}

