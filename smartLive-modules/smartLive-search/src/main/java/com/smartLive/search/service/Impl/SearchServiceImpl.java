package com.smartLive.search.service.Impl;

import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.service.ISearchService;
import com.smartLive.search.utils.EsTool;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {

    @Autowired
    private RestHighLevelClient client;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ==================== 插入数据方法 ====================

    /**
     * 插入或更新文档
     */
    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        IndexRequest request = new IndexRequest(indexName);
        request.id(id);

        Map<String, Object> jsonMap = EsTool.convertToJsonMap(data);
        request.source(jsonMap, XContentType.JSON);
        log.info("插入数据：{}", jsonMap);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        return response.status().getStatus() == 201 || response.status().getStatus() == 200;
    }

    /**
     * 批量插入
     */
    @Override
    public boolean batchInsert(String indexName, List<? extends Object> dataList,
                               Function<Object, String> idGenerator) throws IOException {
        log.info("批量插入数据");
        BulkRequest bulkRequest = new BulkRequest();
//         dataList = ConvertToDocListTool.convertToDocList(indexName, (List<Object>) dataList);
        for (Object data : dataList) {
            log.info("数据：{}", data);
            String id = idGenerator.apply(data);
            IndexRequest request = new IndexRequest(indexName).id(id);
            Map<String, Object> jsonMap = EsTool.convertToJsonMap(data);
            log.info("插入数据：{}", jsonMap);
            request.source(jsonMap, XContentType.JSON);
            bulkRequest.add(request);
        }

        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("批量插入结果：{}", response.status().getStatus());
        return !response.hasFailures();
    }

    /**
     * 删除文档
     */
    @Override
    public boolean delete(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        return response.status().getStatus() == 200;
    }


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
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 搜索店铺
     */
    @Override
    public SearchResponse searchShops(FilterSearchRequest searchRequest) throws IOException {
        SearchRequest request = new SearchRequest("shop_index");
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

        sourceBuilder.from((searchRequest.getPage() - 1) * searchRequest.getSize());
        sourceBuilder.size(searchRequest.getSize());

        request.source(sourceBuilder);
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
        return client.search(request, RequestOptions.DEFAULT);
    }
    /**
     * 插入用户搜索历史
     */
    @Override
    public Boolean insertSearchHistory(Long userId,String keyword){
        String key = RedisConstants.SEARCH_HISTORY_KEY + userId;
        double score = System.currentTimeMillis();
        // 先删除已存在的相同关键词
        stringRedisTemplate.opsForZSet().remove(key, keyword);
        // 添加新记录
        stringRedisTemplate.opsForZSet().add(key, keyword, score);

        // 保持最近10条
        stringRedisTemplate.opsForZSet().removeRange(key, 0, -11);

        // 设置30天过期
        stringRedisTemplate.expire(key, RedisConstants.SEARCH_HISTORY_TTL, TimeUnit.DAYS);
        return true;
    }
    /**
     * 记录搜索关键字
     */
    public Boolean recordSearch(String keyword){
        String key = RedisConstants.SEARCH_HOT_KEYWORDS;
        stringRedisTemplate.opsForZSet().incrementScore(key, keyword, 1);
        // 设置24小时过期
        stringRedisTemplate.expire(key, RedisConstants.SEARCH_HOT_TTL, TimeUnit.HOURS);
        return true;
    }
}
