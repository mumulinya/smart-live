package com.smartLive.search.service.Impl;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.domain.req.UserFilterSearchRequest;
import com.smartLive.search.service.ISearchService;
import com.smartLive.search.service.IUserSearchService;
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
public class UserSearchServiceImpl implements IUserSearchService {

    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private RedisService redisService;
    /**
     * 带过滤条件的搜索
     */
    @Override
    public SearchResponse searchWithFilter(UserFilterSearchRequest userFilterSearchRequest) throws IOException {
        String indexName = EsIndexNameConstants.USER_RESOURCE_INDEX_NAME;
        String keyword = userFilterSearchRequest.getKeyword();
        Integer page = userFilterSearchRequest.getPage();
        Integer size = userFilterSearchRequest.getSize();
        SearchRequest request = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String[] searchFields = EsTool.getDefaultSearchFields(userFilterSearchRequest.getSourceType());
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, searchFields));
        }
        // 过滤条件
        boolQuery.filter(QueryBuilders.matchQuery("sourceType", userFilterSearchRequest.getSourceType()));
        boolQuery.filter(QueryBuilders.matchQuery("actionType", userFilterSearchRequest.getActionType()));

        sourceBuilder.query(boolQuery);
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);
        request.source(sourceBuilder);
        //设置高亮
        request.source().highlighter(createHighlightBuilder(userFilterSearchRequest.getSourceType()));
        return client.search(request, RequestOptions.DEFAULT);
    }
    /**
     * 创建高亮构建器
     */
    public HighlightBuilder createHighlightBuilder(Integer type){
        //设置高亮
        HighlightBuilder highlightBuilder=new HighlightBuilder();
        String[] fields = EsTool.getDefaultSearchFields(type);
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
        String key= RedisConstants.SEARCH_USER_HISTORY_KEY+userId;
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
}
