package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

/**
 * 评分维度排序策略
 * 纯粹基于商户的评级分数进行倒序排列，适用于“好评优先”场景。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component("scoreShopSortStrategy")
public class ScoreShopSortStrategy implements ShopSortStrategy {
    @Override
    public String getSortType() {
        return "score";
    }

    @Override
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request) {
        sourceBuilder.query(boolQuery);
        // 执行 ES 字段排序：按评分（score）降序排列
        sourceBuilder.sort(SortBuilders.fieldSort("score").order(SortOrder.DESC));
    }
}
