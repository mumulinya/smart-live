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

/**
 * ES 响应结果转换器
 * 负责从 SearchResponse 中解析原始数据、高亮片段以及计算业务相关的动态字段（如距离）。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
public class ResponseConverter {

    private ResponseConverter() {}
    
    /**
     * 解析 ES 响应并填充博客列表
     * 支持 Title、Content 和博主昵称 (Name) 的高亮片段替换。
     */
    public static List<BlogDoc> convertToBlogList(SearchResponse response) {
        List<BlogDoc> blogs = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            BlogDoc blog = JSON.parseObject(source, BlogDoc.class);
            // 提取高亮字段并回填至实体
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.BLOG_INDEX_NAME);
                for (String field : fields) {
                    HighlightField hf = highlightFields.get(field);
                    if (hf != null) {
                        String text = hf.getFragments()[0].toString();
                        if ("title".equals(field)) blog.setTitle(text);
                        else if ("content".equals(field)) blog.setContent(text);
                        else if ("name".equals(field)) blog.setName(text);
                    }
                }
            }
            blogs.add(blog);
        }
        return blogs;
    }
    
    /**
     * 解析 ES 响应并填充店铺列表
     * 包含 LBS 距离重算：根据请求的经纬度与店铺坐标，实时计算两者之间的直线球面距离（Haversin 公式）。
     */
    public static List<ShopDoc> convertToShopList(SearchResponse response, FilterSearchRequest request) {
        List<ShopDoc> shops = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            ShopDoc shop = JSON.parseObject(source, ShopDoc.class);
            // 实时计算距离 (单位：米)
            if (request != null && shop.getX() != null && shop.getY() != null) {
                double distance = SloppyMath.haversinMeters(request.getLat(), request.getLon(), shop.getY(), shop.getX());
                shop.setDistance(distance);
            }

            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.SHOP_INDEX_NAME);
                for (String field : fields) {
                    HighlightField hf = highlightFields.get(field);
                    if (hf != null) {
                        String text = hf.getFragments()[0].toString();
                        if ("name".equals(field)) shop.setName(text);
                        else if ("address".equals(field)) shop.setAddress(text);
                        else if ("area".equals(field)) shop.setArea(text);
                    }
                }
            }
            shops.add(shop);
        }
        return shops;
    }
    
    /**
     * 解析并填充用户检索列表
     */
    public static List<UserDoc> convertToUserList(SearchResponse response) {
        List<UserDoc> users = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            UserDoc user = JSON.parseObject(source, UserDoc.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.USER_INDEX_NAME);
                for (String field : fields) {
                    HighlightField hf = highlightFields.get(field);
                    if (hf != null) {
                        String text = hf.getFragments()[0].toString();
                        if ("nickName".equals(field)) user.setNickName(text);
                        else if ("introduce".equals(field)) user.setIntroduce(text);
                        else if ("city".equals(field)) user.setCity(text);
                    }
                }
            }
            users.add(user);
        }
        return users;
    }
    
    /**
     * 解析并填充商品列表
     */
    public static List<ProductDoc> convertToProductList(SearchResponse response) {
        List<ProductDoc> products = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            ProductDoc product = JSON.parseObject(source, ProductDoc.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.PRODUCT_INDEX_NAME);
                for (String field : fields) {
                    HighlightField hf = highlightFields.get(field);
                    if (hf != null) {
                        String text = hf.getFragments()[0].toString();
                        if ("name".equals(field)) product.setName(text);
                        else if ("subTitle".equals(field)) product.setSubTitle(text);
                        else if ("shopName".equals(field)) product.setShopName(text);
                    }
                }
            }
            products.add(product);
        }
        return products;
    }
    
    /**
     * 构建标准分页结果集
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
     * 距离展格式化工具
     * 将米级数字转化为符合用户阅读习惯的 "m" 或 "km"。
     */
    private static String formatDistance(double distanceMeters) {
        if (distanceMeters < 1000) return (int) distanceMeters + "m";
        return String.format("%.1fkm", distanceMeters / 1000.0);
    }
}