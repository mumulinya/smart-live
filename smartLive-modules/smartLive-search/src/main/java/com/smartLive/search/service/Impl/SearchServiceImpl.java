package com.smartLive.search.service.Impl;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.service.ISearchService;
import com.smartLive.search.strategy.factory.ShopSortStrategyFactory;
import com.smartLive.search.strategy.shopSort.ShopSortStrategy;
import com.smartLive.search.utils.EsTool;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 搜索业务实现类
 * 封装了对 Elasticsearch Java Rest High Level Client 的底层操作，包括多字段全文检索、LBS 地理位置筛选及聚合排序逻辑。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {

    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private RedisService redisService;
    @Autowired
    private  ShopSortStrategyFactory shopSortStrategyFactory;

    /**
     * 基础关键词检索
     * 实现跨索引字段的 MultiMatch 查询，支持分页输出及红色高亮展示。
     *
     * @param indexName 索引名
     * @param keyword 关键字 (为空则匹配所有)
     * @param page 页码
     * @param size 每页大小
     */
    @Override
    public SearchResponse search(String indexName, String keyword, int page, int size) throws IOException {
        SearchRequest request = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 获取索引预定义的默认搜索字段
            String[] searchFields = EsTool.getDefaultSearchFields(indexName);
            sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, searchFields));
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);

        request.source(sourceBuilder);
        // 配置搜索结果高亮组件
        request.source().highlighter(createHighlightBuilder(indexName));
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 店铺高级检索 (带坐标及动态排序)
     * 实现地理围栏过滤 (geo_distance) 并整合策略模式处理不同的排序权重逻辑。
     * 
     * @param searchRequest 包含经纬度、距离阈值及业务过滤条件的 DTO
     */
    @Override
    public SearchResponse searchShops(FilterSearchRequest searchRequest) throws IOException {
        SearchRequest request = new SearchRequest(EsIndexNameConstants.SHOP_INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

      // 执行地理位置半径过滤
      if(!searchRequest.getDistance().equals("all") && !searchRequest.getDistance().equals("") 
              && searchRequest.getLat() != null && searchRequest.getLon() != null){
          boolQuery.filter(QueryBuilders.geoDistanceQuery("location")
                  .point(searchRequest.getLat(), searchRequest.getLon())
                  .distance(searchRequest.getDistance()));
      }
      
        // 店铺多字段模糊匹配
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            boolQuery.must(QueryBuilders.multiMatchQuery(searchRequest.getKeyword(), "name", "area", "address"));
        }

        // 动态过滤：支持评分范围与价格上限筛选
        if (searchRequest.getFilters() != null) {
            for (Map.Entry<String, Object> filter : searchRequest.getFilters().entrySet()) {
                if (filter.getValue() != null) {
                    if (filter.getKey().equals("minScore")) {
                        boolQuery.filter(QueryBuilders.rangeQuery("score").gte(filter.getValue()));
                    } else if(filter.getKey().equals("maxPrice")){
                        boolQuery.filter(QueryBuilders.rangeQuery("avgPrice").lte(filter.getValue()));
                    } else {
                        // 精确匹配过滤 (Term)
                        boolQuery.filter(QueryBuilders.termQuery(filter.getKey(), filter.getValue()));
                    }
                }
            }
        }

        sourceBuilder.query(boolQuery);

        // 利用策略工厂获取对应的排序构建器 (距离、评分等)
        ShopSortStrategy strategy = shopSortStrategyFactory.getStrategy(searchRequest.getSortBy());
        strategy.buildSortAndQuery(sourceBuilder, boolQuery, searchRequest);
        
        sourceBuilder.from((searchRequest.getPage() - 1) * searchRequest.getSize());
        sourceBuilder.size(searchRequest.getSize());
        request.source(sourceBuilder);
        request.source().highlighter(createHighlightBuilder(EsIndexNameConstants.SHOP_INDEX_NAME));
        
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 带业务过滤条件的通用搜索
     * 适用于博客标签、商品分类等需要精准分类+模糊搜索的场景。
     */
    @Override
    public SearchResponse searchWithFilter(String indexName, String keyword,
                                           Map<String, Object> filters, int page, int size) throws IOException {
        SearchRequest request = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String[] searchFields = EsTool.getDefaultSearchFields(indexName);
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, searchFields));
        }

        if (filters != null) {
            for (Map.Entry<String, Object> filter : filters.entrySet()) {
                if (filter.getValue() != null) {
                    boolQuery.filter(QueryBuilders.termQuery(filter.getKey(), filter.getValue()));
                }
            }
        }
        sourceBuilder.query(boolQuery);
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);
        request.source(sourceBuilder);
        request.source().highlighter(createHighlightBuilder(indexName));
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 构造 ES 高亮展示配置
     * 为搜索命中的关键字添加全局 CSS 样式（红色加粗）。
     */
    public HighlightBuilder createHighlightBuilder(String indexName){
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        String[] fields = EsTool.getDefaultSearchFields(indexName);
        for (String field : fields) {
            highlightBuilder.field(field);
            highlightBuilder.preTags("<span style='color: red; font-size: inherit;'>");
            highlightBuilder.postTags("</span>");
        }
        return highlightBuilder;
    }

    /**
     * 记录并维护用户搜索历史 (Redis ZSet)
     * 使用时间戳作为 Score，保证列表按时间倒序排列，并限制存储容量。
     */
    @Override
    public Boolean insertSearchHistory(Long userId, String keyword){
        String key = RedisConstants.SEARCH_INDEX_HISTORY_KEY + userId;
        double score = System.currentTimeMillis();
        // 移除重复项，保证置顶更新
        redisService.removeCacheZSetObject(key, keyword);
        // 添加新记录
        redisService.setCacheZSet(key, keyword, score);

        // 仅保留最近 10 条历史数据
        redisService.removeRangeCacheZSetObject(key, 0, -11);
        // 延长过期时间
        redisService.expire(key, RedisConstants.SEARCH_HISTORY_TTL, TimeUnit.DAYS);
        return true;
    }

    /**
     * 热搜词词频统计
     * 利用 Redis ZSet 的 incrementScore 特性，统计全站活跃关键词。
     */
    @Override
    public Boolean recordSearch(String keyword){
        String key = RedisConstants.SEARCH_HOT_KEYWORDS;
        redisService.incrementCacheZSetScore(key, keyword, 1);
        redisService.expire(key, RedisConstants.SEARCH_HOT_TTL, TimeUnit.HOURS);
        return true;
    }
}
