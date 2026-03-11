package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

/**
 * 价格维度排序策略
 * 用于实现“人均消费最低”等基于数值字段的直接排序。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component("priceShopSortStrategy")
public class PriceShopSortStrategy implements ShopSortStrategy {
    @Override
    public String getSortType() {
        return "price";
    }

    @Override
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request) {
        sourceBuilder.query(boolQuery);
        // 执行 ES 字段排序：按人均价格（avgPrice）升序排列
        sourceBuilder.sort(SortBuilders.fieldSort("avgPrice").order(SortOrder.ASC));
    }
}
