package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

/**
 * 地理位置距离排序策略
 * 实现“离我最近”搜索。利用 ES 的 GeoDistanceSort 对地理坐标点（Location）进行实时距离换算并升序排列。
 */
@Component
public class DistanceShopSortStrategy implements ShopSortStrategy {

    @Override
    public String getSortType() {
        return "distance";
    }

    @Override
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest searchRequest) {
        if (searchRequest.getLat() != null && searchRequest.getLon() != null) {
            // 在 ES 的 search_after 或打分阶段注入地理坐标排序
            sourceBuilder.sort(SortBuilders.geoDistanceSort("location", searchRequest.getLat(), searchRequest.getLon())
                    .order(SortOrder.ASC)
                    .unit(DistanceUnit.METERS));
        }
    }
}