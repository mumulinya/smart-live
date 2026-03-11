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

/**
 * 智能热门排序策略
 * 核心逻辑：采用 Function Score 查询，结合地理位置和店铺评分进行综合算分。
 * 1. 地理位置：使用高斯衰减函数 (Gauss Decay)，3km内分值最高，随距离增加分值呈高斯曲线下降。
 * 2. 店铺评分：使用 Field Value Factor，对 score 字段取对数 (LOG1P) 处理并加权，作为人气支撑。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component("hotShopSortStrategy")
public class HotShopSortStrategy implements ShopSortStrategy {
    @Override
    public String getSortType() {
        return "hot";
    }

    @Override
    public void buildSortAndQuery(SearchSourceBuilder sourceBuilder, BoolQueryBuilder boolQuery, FilterSearchRequest request) {
        if (request.getLat() != null && request.getLon() != null) {
            // 构造多维算分函数列表
            FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
                // A. 距离衰减函数：离用户越近，权重越高
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    ScoreFunctionBuilders.gaussDecayFunction(
                            "location", 
                            new org.elasticsearch.common.geo.GeoPoint(request.getLat(), request.getLon()),
                            "3km", "1km", 0.5
                    )
                ),
                // B. 评分加成函数：评分越高，基础分越高
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    ScoreFunctionBuilders.fieldValueFactorFunction("score")
                            .modifier(FieldValueFactorFunction.Modifier.LOG1P)
                            .factor(1.5f)
                )
            };
            
            // 整合算分查询：采用求和模式，并将结果与原始相关性分值相乘
            FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery, functions)
                    .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
                    .boostMode(CombineFunction.MULTIPLY);
                    
            sourceBuilder.query(functionScoreQuery);
        } else {
            // 缺失位置信息时，退化为普通布尔检索
            sourceBuilder.query(boolQuery);
        }
        // 最终基于 ES 计算出的综合 _score 进行倒序排列
        sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
    }
}
