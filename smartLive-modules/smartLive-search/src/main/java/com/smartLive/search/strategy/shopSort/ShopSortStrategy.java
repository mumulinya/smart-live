package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * ES 店铺搜索排序策略接口
 */
public interface ShopSortStrategy {

    /**
     * 获取该策略对应的排序类型标识 (例如: "hot", "distance", "score")
     */
    String getSortType();

    /**
     * 构建查询与排序条件
     *
     * @param sourceBuilder ES 的 SearchSourceBuilder
     * @param boolQuery     已经构建好基础过滤条件的 BoolQueryBuilder
     * @param request       前端传来的原始搜索请求（包含经纬度等）
     */
    void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request);
}