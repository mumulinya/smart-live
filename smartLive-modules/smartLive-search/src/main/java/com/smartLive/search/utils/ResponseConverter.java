package com.smartLive.search.utils;

import com.alibaba.fastjson.JSON;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.ProductDoc;
import com.smartLive.search.domain.req.FilterSearchRequest;
import org.apache.lucene.util.SloppyMath;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseConverter {

    private ResponseConverter() {
    }
    
    /**
     * 将ES搜索结果转换为博客列表
     */
    public static List<BlogDoc> convertToBlogList(SearchResponse response) {
        List<BlogDoc> blogs = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            //获取source数据
            String source = hit.getSourceAsString();
            //转化为对应的对象
            BlogDoc blog = JSON.parseObject(source, BlogDoc.class);
            //处理高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null&&highlightFields.size()>0) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.BLOG_INDEX_NAME);
                for (String field : fields) {
                    if (field.equals("title")&&highlightFields.get(field) != null) {
                        blog.setTitle(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("content")&&highlightFields.get(field) != null) {
                        blog.setContent(highlightFields.get(field).getFragments()[0].toString());
                    }else if (field.equals("name")&&highlightFields.get(field) != null) {
                        blog.setName(highlightFields.get(field).getFragments()[0].toString());
                    }
                }
            }
            blogs.add(blog);
        }
        return blogs;
    }
    
    /**
     * 将ES搜索结果转换为店铺列表
     */
    public static List<ShopDoc> convertToShopList(SearchResponse response, FilterSearchRequest request) {
        List<ShopDoc> shops = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            ShopDoc shop = JSON.parseObject(source, ShopDoc.class);
            //设置距离
            if(request!=null){
                // 假设 shop.getLocation() 格式是 "lat,lon" 或包含这两者
                double shopLat = shop.getY(); // 提取店铺纬度
                double shopLon = shop.getX(); // 提取店铺经度

                // 使用 Hutool 工具类直接算出距离（精确到米），然后塞给 DTO！
                double distance = SloppyMath.haversinMeters(request.getLat(), request.getLon(), shopLat, shopLon);

                // 格式化一下（比如 1550 米变成 "1.5km"）再传给前端
                shop.setDistance(distance);
            }

            //处理高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null&&highlightFields.size()>0) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.SHOP_INDEX_NAME);
                for (String field : fields) {
                    if (field.equals("name")&&highlightFields.get(field) != null) {
                        shop.setName(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("address")&&highlightFields.get(field) != null) {
                        shop.setAddress(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("area")&&highlightFields.get(field) != null) {
                        shop.setArea(highlightFields.get(field).getFragments()[0].toString());
                    }
                }
            }
            shops.add(shop);
        }
        return shops;
    }
    
    /**
     * 将ES搜索结果转换为用户列表
     */
    public static List<UserDoc> convertToUserList(SearchResponse response) {
        List<UserDoc> users = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            UserDoc user = JSON.parseObject(source, UserDoc.class);
            //处理高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null&&highlightFields.size()>0) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.USER_INDEX_NAME);
                for (String field : fields) {
                    if (field.equals("nickName")&&highlightFields.get(field) != null) {
                        user.setNickName(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("introduce")&&highlightFields.get(field) != null) {
                        user.setIntroduce(highlightFields.get(field).getFragments()[0].toString());
                    }else if (field.equals("city")&&highlightFields.get(field) != null) {
                        user.setCity(highlightFields.get(field).getFragments()[0].toString());
                    }
                }
            }
            users.add(user);
        }
        return users;
    }
    
    /**
     * 将ES搜索结果转换为商品列表
     */
    public static List<ProductDoc> convertToProductList(SearchResponse response) {
        List<ProductDoc> products = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            ProductDoc product = JSON.parseObject(source, ProductDoc.class);
            //处理高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            System.out.println("高亮结果为"+highlightFields);
            if (highlightFields != null&&highlightFields.size()>0) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.PRODUCT_INDEX_NAME);
                for (String field : fields) {
                    if (field.equals("name")&&highlightFields.get(field) != null) {
                        product.setName(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("subTitle")&&highlightFields.get(field) != null) {
                        product.setSubTitle(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("shopName")&&highlightFields.get(field) != null) {
                        product.setShopName(highlightFields.get(field).getFragments()[0].toString());
                    }
                }
            }
            products.add(product);
        }
        return products;
    }
    
    /**
     * 构建分页响应
     */
    public static <T> Map<String, Object> buildPageResult(SearchResponse response, List<T> list) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", response.getHits().getTotalHits().value);
        result.put("pageSize", list.size());
        result.put("took", response.getTook().getMillis() + "ms");
        return result;
    }

    /**
     * 将米转换为前端展示的格式化字符串
     *
     * @param distanceMeters 距离（米）
     * @return 格式化后的字符串，如 "850m" 或 "1.2km"
     */
    private static String formatDistance(double distanceMeters) {
        if (distanceMeters < 1000) {
            // 小于 1 公里，直接显示整数米
            return (int) distanceMeters + "m";
        } else {
            // 大于 1 公里，转换为 km 并保留一位小数
            double km = distanceMeters / 1000.0;
            return String.format("%.1fkm", km);
        }
    }
}