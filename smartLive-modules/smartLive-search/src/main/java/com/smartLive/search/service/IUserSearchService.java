package com.smartLive.search.service;

import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.domain.req.UserFilterSearchRequest;
import org.elasticsearch.action.search.SearchResponse;

import java.io.IOException;
import java.util.Map;

public interface IUserSearchService {
    /**
     * 带过滤条件的搜索
     */
    SearchResponse searchWithFilter(UserFilterSearchRequest request) throws IOException;

    /**
     * 记录用户搜索历史
     */
    public Boolean insertSearchHistory(Long userId,String keyword);

}