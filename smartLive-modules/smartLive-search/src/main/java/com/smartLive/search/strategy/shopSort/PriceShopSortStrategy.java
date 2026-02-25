package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

@Component("priceShopSortStrategy")
public class PriceShopSortStrategy implements ShopSortStrategy {
    /**
     * 获取该策略对应的排序类型标识 (例如: "hot", "distance", "score")
     */
    @Override
    public String getSortType() {
        return "price";
    }

    @Override
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request) {
        sourceBuilder.query(boolQuery);
        // 按人均消费从低到高排
        sourceBuilder.sort(SortBuilders.fieldSort("avgPrice").order(SortOrder.ASC));
    }
}