package com.smartLive.search.utils;

import com.alibaba.fastjson.JSON;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.VoucherDoc;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseConverter {
    
    /**
     * 将ES搜索结果转换为博客列表
     */
    public static List<BlogDoc> convertToBlogList(SearchResponse response) {
        List<BlogDoc> blogs = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            BlogDoc blog = JSON.parseObject(source, BlogDoc.class);
            blogs.add(blog);
        }
        return blogs;
    }
    
    /**
     * 将ES搜索结果转换为店铺列表
     */
    public static List<ShopDoc> convertToShopList(SearchResponse response) {
        List<ShopDoc> shops = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            ShopDoc shop = JSON.parseObject(source, ShopDoc.class);
            
            // 处理距离信息（如果有）
            Object[] sortValues = hit.getSortValues();
            if (sortValues != null && sortValues.length > 0) {
                double distance = Double.parseDouble(sortValues[0].toString());
                shop.setDistance(distance);
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
            users.add(user);
        }
        return users;
    }
    
    /**
     * 将ES搜索结果转换为优惠券列表
     */
    public static List<VoucherDoc> convertToVoucherList(SearchResponse response) {
        List<VoucherDoc> vouchers = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            VoucherDoc voucher = JSON.parseObject(source, VoucherDoc.class);
            vouchers.add(voucher);
        }
        return vouchers;
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
     * 转换搜索结果
     */
    public static Object convertSearchResult(String indexName, SearchResponse response) {
        switch (indexName) {
            case "blog_index":
                return convertToBlogList(response);
            case "shop_index":
                return convertToShopList(response);
            case "user_index":
                return convertToUserList(response);
            case "voucher_index":
                return convertToVoucherList(response);
            default:
                // 返回原始命中数据
                List<Map<String, Object>> result = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    result.add(hit.getSourceAsMap());
                }
                return result;
        }
    }
}