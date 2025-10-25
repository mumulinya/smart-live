package com.smartLive.search.service.Impl;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class SimpleSearchService {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 简单搜索 - 最常用的搜索方式
     */
    public SearchResponse search(String indexName, String keyword, int page, int size) throws IOException {
        SearchRequest request = new SearchRequest(indexName);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 根据索引类型设置不同的搜索字段
            String[] searchFields = getDefaultSearchFields(indexName);
            sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, searchFields));
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }
        
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);
        
        request.source(sourceBuilder);
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 搜索附近店铺
     */
    public SearchResponse searchNearbyShops(double lon, double lat, String distance, 
                                          String keyword, int page, int size) throws IOException {
        SearchRequest request = new SearchRequest("shop_index");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        
        // 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // 距离过滤
        boolQuery.filter(QueryBuilders.geoDistanceQuery("location")
                .point(lat, lon)
                .distance(distance));
        
        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, "name", "area", "address"));
        }
        
        sourceBuilder.query(boolQuery);
        
        // 按距离排序
        sourceBuilder.sort(SortBuilders.geoDistanceSort("location", lat, lon)
                .order(SortOrder.ASC)
                .unit(DistanceUnit.METERS));
        
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);
        
        request.source(sourceBuilder);
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 带过滤条件的搜索
     */
    public SearchResponse searchWithFilter(String indexName, String keyword,
                                           Map<String, Object> filters, int page, int size) throws IOException {
        SearchRequest request = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String[] searchFields = getDefaultSearchFields(indexName);
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, searchFields));
        }
        
        // 过滤条件
        if (filters != null) {
            for (Map.Entry<String, Object> filter : filters.entrySet()) {
                if (filter.getValue() != null) {
                    boolQuery.filter(QueryBuilders.termQuery(filter.getKey(), filter.getValue()));
                }
            }
        }
        
        sourceBuilder.query(boolQuery);
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);
        
        request.source(sourceBuilder);
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 获取默认搜索字段
     */
    private String[] getDefaultSearchFields(String indexName) {
        switch (indexName) {
            case "blog_index":
                return new String[]{"title", "content", "name"};
            case "shop_index":
                return new String[]{"name", "area", "address"};
            case "user_index":
                return new String[]{"nickName", "introduce", "city"};
            case "voucher_index":
                return new String[]{"title", "subTitle", "rules", "shopName"};
            default:
                return new String[]{}; // 匹配所有字段
        }
    }
}