package com.smartLive.search.controller;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.domain.req.UserFilterSearchRequest;
import com.smartLive.search.domain.res.SearchResult;
import com.smartLive.search.service.ISearchService;
import com.smartLive.search.service.IUserSearchService;
import com.smartLive.search.utils.EsTool;
import com.smartLive.search.utils.ResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/search/user")
@Slf4j
public class UserSearchController {
    @Autowired
    private IUserSearchService userSearchService;
    @Autowired
    private RedisService redisService;
    @GetMapping
    public ResponseEntity<Object> searchWithFilter(UserFilterSearchRequest request) {
        try {
            SearchResponse response = userSearchService.searchWithFilter(request);
            List<?> dataList = EsTool.convertSearchResult(request.getSourceType(), response);
            Map<String, Object> result = ResponseConverter.buildPageResult(response, dataList);
            return ResponseEntity.ok(SearchResult.success(result));
        } catch (Exception e) {
            log.error("搜索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("搜索失败: " + e.getMessage()));
        }
    }

    // 添加搜索历史（去重）
    @PostMapping("/history")
    public Result addSearchHistory(@RequestParam("userId") Long userId, @RequestParam("keyword") String keyword) {
        try {
            userSearchService.insertSearchHistory(userId, keyword);
            return Result.ok();
        } catch (Exception e) {
            log.error("添加搜索历史失败, userId: {}, keyword: {}",
                    userId, keyword, e);
            return Result.fail("添加搜索历史失败");
        }
    }
    // 获取历史搜索
    @GetMapping("/history")
    public Result getSearchHistory(@RequestParam("userId") Long userId) {
        try {
            String key= RedisConstants.SEARCH_USER_HISTORY_KEY+userId;
            Set<Object> history = redisService.getCacheZSetReverseRange(key, 0, 9);
            return Result.ok(new ArrayList<>(history));
        } catch (Exception e) {
            log.error("获取历史搜索失败, userId: {}", userId, e);
            return Result.fail("获取历史搜索失败");
        }
    }
    // 清空用户搜索历史
    @DeleteMapping("/history")
    public Result clearSearchHistory(@RequestParam("userId") Long userId) {
        try {
            String key= RedisConstants.SEARCH_USER_HISTORY_KEY+userId;
            redisService.deleteObject(key);
            return Result.ok("搜索历史已清空");
        } catch (Exception e) {
            log.error("清空搜索历史失败, userId: {}", userId, e);
            return Result.fail("清空搜索历史失败");
        }
    }
}
