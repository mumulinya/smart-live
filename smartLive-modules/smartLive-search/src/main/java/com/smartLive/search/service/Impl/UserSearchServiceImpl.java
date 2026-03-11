package com.smartLive.search.service.Impl;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.search.domain.req.UserFilterSearchRequest;
import com.smartLive.search.service.IUserSearchService;
import com.smartLive.search.utils.EsTool;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 用户资源搜索业务实现类
 * 负责在 user_resource 宽表索引中检索用户相关的社交动态、收藏和点赞数据。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Service
@Slf4j
public class UserSearchServiceImpl implements IUserSearchService {

    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private RedisService redisService;

    /**
     * 执行带业务过滤的用户资源搜索
     * 整合了业务类型 (sourceType) 和动作类型 (actionType) 的多重 Match 过滤。
     *
     * @param userFilterSearchRequest 搜索筛选 DTO
     * @return ES 原始搜索响应
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

        // 1. 关键词全文检索 (针对不同资源类型对应的默认搜索字段)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String[] searchFields = EsTool.getDefaultSearchFields(userFilterSearchRequest.getSourceType());
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, searchFields));
        }
        
        // 2. 核心业务维度过滤
        // 筛选资源类型 (如：博客、店铺)
        boolQuery.filter(QueryBuilders.matchQuery("sourceType", userFilterSearchRequest.getSourceType()));
        // 筛选动作类型 (如：点赞、收藏)
        boolQuery.filter(QueryBuilders.matchQuery("actionType", userFilterSearchRequest.getActionType()));

        sourceBuilder.query(boolQuery);
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);
        request.source(sourceBuilder);
        
        // 3. 配置高亮展示效果
        request.source().highlighter(createHighlightBuilder(userFilterSearchRequest.getSourceType()));
        
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * 构建 ES 高亮配置
     * 自动适配不同资源类型对应的搜索字段。
     */
    public HighlightBuilder createHighlightBuilder(Integer type){
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        String[] fields = EsTool.getDefaultSearchFields(type);
        for (String field : fields) {
            highlightBuilder.field(field);
            highlightBuilder.preTags("<span style='color: red; font-size: inherit;'>");
            highlightBuilder.postTags("</span>");
        }
        return highlightBuilder;
    }

    /**
     * 维护用户个人在社交领域的搜索历史 (Redis ZSet)
     * 实现记录置顶、自动去重及固定窗口（最近 10 条）管理。
     */
    @Override
    public Boolean insertSearchHistory(Long userId, String keyword){
        String key = RedisConstants.SEARCH_USER_HISTORY_KEY + userId;
        double score = System.currentTimeMillis();
        
        // 移除旧记录保证新搜索置顶
        redisService.removeCacheZSetObject(key, keyword);
        // 插入当前搜索词
        redisService.setCacheZSet(key, keyword, score);

        // 维持列表长度在 10 条以内
        redisService.removeRangeCacheZSetObject(key, 0, -11);
        // 默认保留 30 天
        redisService.expire(key, RedisConstants.SEARCH_HISTORY_TTL, TimeUnit.DAYS);
        return true;
    }
}
