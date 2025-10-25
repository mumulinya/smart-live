package com.smartLive.search.service.Impl;

import com.alibaba.fastjson.JSON;
import com.smartLive.search.domain.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UnifiedDataInsertService {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 统一插入数据到ES索引
     * @param indexType 索引类型
     * @param data 数据对象
     * @return 是否插入成功
     */
    public boolean insertToEs(IndexType indexType, Object data) throws IOException {
        String indexName = indexType.getIndexName();
        String id = getDocumentId(indexType, data);
        
        IndexRequest request = new IndexRequest(indexName);
        request.id(id);
        
        // 根据不同类型处理数据
        String jsonData = convertToJson(indexType, data);
        request.source(jsonData, XContentType.JSON);
        
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        return response.status().getStatus() == 201 || response.status().getStatus() == 200;
    }

    /**
     * 批量插入数据
     */
    public boolean batchInsertToEs(IndexType indexType, List<?> dataList) throws IOException {
        for (Object data : dataList) {
            boolean success = insertToEs(indexType, data);
            if (!success) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取文档ID
     */
    private String getDocumentId(IndexType indexType, Object data) {
        switch (indexType) {
            case BLOG:
                return ((BlogDoc) data).getId().toString();
            case SHOP:
                return ((ShopDoc) data).getId().toString();
            case USER:
                return ((UserDoc) data).getId().toString();
            case VOUCHER:
                return ((VoucherDoc) data).getId().toString();
            default:
                throw new IllegalArgumentException("不支持的索引类型: " + indexType);
        }
    }

    /**
     * 转换为JSON，处理特殊字段
     */
    private String convertToJson(IndexType indexType, Object data) {
        Map<String, Object> jsonMap = new HashMap<>();
        
        switch (indexType) {
            case BLOG:
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
                break;
                
            case SHOP:
                ShopDoc shop = (ShopDoc) data;
                jsonMap.put("id", shop.getId());
                jsonMap.put("name", shop.getName());
                jsonMap.put("typeId", shop.getTypeId());
                jsonMap.put("images", shop.getImages());
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
                if (shop.getCreateTime() != null) {
                    jsonMap.put("createTime", shop.getCreateTime().getTime());
                }
                break;
                
            case USER:
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
                break;
                
            case VOUCHER:
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
                break;
                
            default:
                throw new IllegalArgumentException("不支持的索引类型: " + indexType);
        }
        
        return JSON.toJSONString(jsonMap);
    }

    /**
     * 索引类型枚举
     */
    public enum IndexType {
        BLOG("blog_index"),
        SHOP("shop_index"),
        USER("user_index"),
        VOUCHER("voucher_index");

        private final String indexName;

        IndexType(String indexName) {
            this.indexName = indexName;
        }

        public String getIndexName() {
            return indexName;
        }
    }
}