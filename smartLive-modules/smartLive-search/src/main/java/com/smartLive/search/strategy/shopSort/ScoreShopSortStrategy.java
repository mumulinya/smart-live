package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

@Component("scoreShopSortStrategy")
public class ScoreShopSortStrategy implements ShopSortStrategy {
    public String getSortType() {
        return "score";
    }
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request) {
        sourceBuilder.query(boolQuery);
        // 按店铺评分从高到低排
        sourceBuilder.sort(SortBuilders.fieldSort("score").order(SortOrder.DESC));
    }
}