package com.smartLive.search.service;

import com.smartLive.search.domain.req.FilterSearchRequest;
import org.elasticsearch.action.search.SearchResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface ISearchService {

    // ==================== 插入数据方法 ====================

    /**
     * 插入或更新文档
     */
    boolean insertOrUpdate(String indexName, String id, Object data) throws IOException;

    /**
     * 批量插入
     */
    boolean batchInsert(String indexName, List<? extends Object> dataList,
                        Function<Object, String> idGenerator) throws IOException;

    /**
     * 删除文档
     */
    boolean delete(String indexName, String id) throws IOException;


    /**
     * 简单搜索
     */
    SearchResponse search(String indexName, String keyword, int page, int size) throws IOException;

    /**
     * 搜索附近店铺
     */
    SearchResponse searchShops(FilterSearchRequest searchRequest) throws IOException;

    /**
     * 带过滤条件的搜索
     */
    SearchResponse searchWithFilter(String indexName, String keyword,
                                    Map<String, Object> filters, int page, int size) throws IOException;

    /**
     * 记录用户搜索历史
     */
    public Boolean insertSearchHistory(Long userId,String keyword);

    /**
     * 记录搜索关键字
     */
    public Boolean recordSearch(String keyword);
    }