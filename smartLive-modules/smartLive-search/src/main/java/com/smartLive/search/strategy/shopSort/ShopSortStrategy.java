package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * 店铺排序算法策略接口
 * 专门用于在“周边/分类店铺搜索”场景下，根据用户选择的维度（距离、评分、销量、价格等）构建 ES 排序逻辑。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
public interface ShopSortStrategy {

    /**
     * 获取排序策略的标识名称 (如：distance, score, hot)
     */
    String getSortType();

    /**
     * 向 ES 查询请求中注入特定的排序及辅助过滤逻辑
     * 
     * @param sourceBuilder ES 搜索源构建器 (负责注入 Sort)
     * @param boolQuery ES 布尔查询构建器 (负责注入相关过滤，如地理位置范围)
     * @param searchRequest 原始前端请求 DTO (包含坐标、排序规则)
     */
    void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest searchRequest);
}
