package com.smartLive.search.utils;

import com.alibaba.fastjson.JSON;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.VoucherDoc;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Component;

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
     * 将ES搜索结果转换为优惠券列表
     */
    public static List<VoucherDoc> convertToVoucherList(SearchResponse response) {
        List<VoucherDoc> vouchers = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            VoucherDoc voucher = JSON.parseObject(source, VoucherDoc.class);
            //处理高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            System.out.println("高亮结果为"+highlightFields);
            if (highlightFields != null&&highlightFields.size()>0) {
                String[] fields = EsTool.getDefaultSearchFields(EsIndexNameConstants.VOUCHER_INDEX_NAME);
                for (String field : fields) {
                    if (field.equals("title")&&highlightFields.get(field) != null) {
                        voucher.setTitle(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("subTitle")&&highlightFields.get(field) != null) {
                        voucher.setSubTitle(highlightFields.get(field).getFragments()[0].toString());
                    } else if (field.equals("shopName")&&highlightFields.get(field) != null) {
                        voucher.setShopName(highlightFields.get(field).getFragments()[0].toString());
                    }
                }
            }
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
            case EsIndexNameConstants.BLOG_INDEX_NAME:
                return convertToBlogList(response);
            case EsIndexNameConstants.SHOP_INDEX_NAME:
                return convertToShopList(response);
            case EsIndexNameConstants.USER_INDEX_NAME:
                return convertToUserList(response);
            case EsIndexNameConstants.VOUCHER_INDEX_NAME:
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