package com.smartLive.search.service.Impl;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.service.ISearchService;
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

@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {

    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private RedisService redisService;
    /**
     * 简单搜索
     */
    @Override
    public SearchResponse search(String indexName, String keyword, int page, int size) throws IOException {
        SearchRequest request = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String[] searchFields = EsTool.getDefaultSearchFields(indexName);
            sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, searchFields));
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);

        request.source(sourceBuilder);
        //设置高亮
        request.source().highlighter(createHighlightBuilder(indexName));
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 搜索店铺
     */
    @Override
    public SearchResponse searchShops(FilterSearchRequest searchRequest) throws IOException {
        SearchRequest request = new SearchRequest(EsIndexNameConstants.SHOP_INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

      if(!searchRequest.getDistance().equals("all")&&!searchRequest.getDistance().equals("")&&searchRequest.getLat()!=null&&searchRequest.getLon()!=null){
          // 距离过滤
          boolQuery.filter(QueryBuilders.geoDistanceQuery("location")
                  .point(searchRequest.getLat(), searchRequest.getLon())
                  .distance(searchRequest.getDistance()));
      }
        // 关键词搜索
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            boolQuery.must(QueryBuilders.multiMatchQuery(searchRequest.getKeyword(), "name", "area", "address"));
        }

        // 过滤条件
        if (searchRequest.getFilters() != null) {
            for (Map.Entry<String, Object> filter : searchRequest.getFilters().entrySet()) {
                if (filter.getValue() != null) {
                    if (filter.getKey().equals("minScore")) {
                        boolQuery.filter(QueryBuilders.rangeQuery("score").gte(filter.getValue()));
                    }else if(filter.getKey().equals("maxPrice")){
                        boolQuery.filter(QueryBuilders.rangeQuery("avgPrice").lte(filter.getValue()));
                    }
                    else{
                        boolQuery.filter(QueryBuilders.termQuery(filter.getKey(), filter.getValue()));
                    }
                }
            }
        }

        sourceBuilder.query(boolQuery);

       if(searchRequest.getLon()!=null&&searchRequest.getLat()!=null){
           // 按距离排序
           sourceBuilder.sort(SortBuilders.geoDistanceSort("location", searchRequest.getLat(), searchRequest.getLon())
                   .order(SortOrder.ASC)
                   .unit(DistanceUnit.METERS));
       }
        //进行分页
        sourceBuilder.from((searchRequest.getPage() - 1) * searchRequest.getSize());
        sourceBuilder.size(searchRequest.getSize());
        request.source(sourceBuilder);
        //设置高亮
        request.source().highlighter(createHighlightBuilder(EsIndexNameConstants.SHOP_INDEX_NAME));
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 带过滤条件的搜索
     */
    @Override
    public SearchResponse searchWithFilter(String indexName, String keyword,
                                           Map<String, Object> filters, int page, int size) throws IOException {
        SearchRequest request = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String[] searchFields = EsTool.getDefaultSearchFields(indexName);
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, searchFields));
        }

        // 过滤条件
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
        //设置高亮
        request.source().highlighter(createHighlightBuilder(indexName));
        return client.search(request, RequestOptions.DEFAULT);
    }
    /**
     * 创建高亮构建器
     */
    public HighlightBuilder createHighlightBuilder(String indexName){
        //设置高亮
        HighlightBuilder highlightBuilder=new HighlightBuilder();
        String[] fields = EsTool.getDefaultSearchFields(indexName);
        for (String field : fields) {
            highlightBuilder.field(field);
            highlightBuilder.preTags("<span  style='color: red; font-size: inherit;'>");
            highlightBuilder.postTags("</span >");
        }
        return highlightBuilder;
    }
    /**
     * 插入用户搜索历史
     */
    @Override
    public Boolean insertSearchHistory(Long userId,String keyword){
        String key= RedisConstants.SEARCH_INDEX_HISTORY_KEY+userId;
        double score = System.currentTimeMillis();
        // 先删除已存在的相同关键词
        redisService.removeCacheZSetObject(key, keyword);
        // 添加新记录
        redisService.setCacheZSet(key, keyword, score);

        // 保持最近10条
        redisService.removeRangeCacheZSetObject(key, 0, -11);
        // 设置30天过期
        redisService.expire(key, RedisConstants.SEARCH_HISTORY_TTL, TimeUnit.DAYS);
        return true;
    }
    /**
     * 记录搜索关键字
     */
    @Override
    public Boolean recordSearch(String keyword){
        String key = RedisConstants.SEARCH_HOT_KEYWORDS;
        redisService.incrementCacheZSetScore(key, keyword, 1);
        // 设置24小时过期
        redisService.expire(key, RedisConstants.SEARCH_HOT_TTL, TimeUnit.HOURS);
        return true;
    }
}
