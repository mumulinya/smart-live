package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

@Component("distanceShopSortStrategy")
public class DistanceShopSortStrategy implements ShopSortStrategy {
    @Override
    public String getSortType() {
        return "distance"; // 专属标识
    }
    @Override
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request) {
        sourceBuilder.query(boolQuery); // 原封不动塞入 boolQuery
        if (request.getLat() != null && request.getLon() != null) {
            sourceBuilder.sort(SortBuilders.geoDistanceSort("location", request.getLat(), request.getLon())
                    .order(SortOrder.ASC)
                    .unit(DistanceUnit.METERS));
        }
    }
}