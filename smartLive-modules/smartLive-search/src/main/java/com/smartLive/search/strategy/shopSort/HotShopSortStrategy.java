package com.smartLive.search.strategy.shopSort;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

@Component("hotShopSortStrategy")
public class HotShopSortStrategy implements ShopSortStrategy {
    @Override
    public String getSortType() {
        return "hot"; // 返回该策略的专属标识
    }
    @Override
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request) {
        if (request.getLat() != null && request.getLon() != null) {
            // 构造高斯距离衰减 + 评分加成的综合算分函数
            FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    ScoreFunctionBuilders.gaussDecayFunction(
                            "location", 
                            new org.elasticsearch.common.geo.GeoPoint(request.getLat(), request.getLon()),
                            "3km", "1km", 0.5
                    )
                ),
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    ScoreFunctionBuilders.fieldValueFactorFunction("score")
                            .modifier(FieldValueFactorFunction.Modifier.LOG1P).factor(1.5f)
                )
            };
            
            FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery, functions)
                    .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
                    .boostMode(CombineFunction.MULTIPLY);
                    
            sourceBuilder.query(functionScoreQuery);
        } else {
            // 没传经纬度，退化为普通布尔查询
            sourceBuilder.query(boolQuery);
        }
        // 最终按计算出的 _score 降序
        sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
    }
}