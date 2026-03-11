package com.smartLive.search.controller;

import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.search.domain.req.UserFilterSearchRequest;
import com.smartLive.search.domain.res.SearchResult;
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

/**
 * 用户资源搜索控制器
 * 专门用于搜索用户关联的动态资源，如“我收藏的店铺”、“我点赞的笔记”等聚合索引数据。
 * 支持基于动作类型 (ActionType) 和业务类型 (SourceType) 的精准过滤。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/search/user")
@Slf4j
public class UserSearchController {
    @Autowired
    private IUserSearchService userSearchService;
    @Autowired
    private RedisService redisService;

    /**
     * 带过滤条件的用户资源搜索
     * 从 user_resource 宽表索引中检索数据。
     * 
     * @param request 包含关键词、业务类型(sourceType)和动作类型(actionType)的请求对象
     * @return 分页后的资源列表
     */
    @GetMapping
    public ResponseEntity<Object> searchWithFilter(UserFilterSearchRequest request) {
        try {
            SearchResponse response = userSearchService.searchWithFilter(request);
            List<?> dataList = EsTool.convertSearchResult(request.getSourceType(), response);
            Map<String, Object> result = ResponseConverter.buildPageResult(response, dataList);
            return ResponseEntity.ok(SearchResult.success(result));
        } catch (Exception e) {
            log.error("用户资源搜索失败: {}", e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 持久化用户社交搜索历史
     * 使用 Redis ZSet 存储，支持自动去重及按时间倒序排列。
     * 
     * @param userId 用户 ID
     * @param keyword 搜索关键词
     */
    @PostMapping("/history")
    public Result addSearchHistory(@RequestParam("userId") Long userId, @RequestParam("keyword") String keyword) {
        try {
            userSearchService.insertSearchHistory(userId, keyword);
            return Result.ok();
        } catch (Exception e) {
            log.error("添加用户搜索历史失败, userId: {}, keyword: {}", userId, keyword, e);
            return Result.fail("添加搜索历史失败");
        }
    }

    /**
     * 获取用户社交搜索历史列表
     * 返回最近的 10 条搜索记录。
     */
    @GetMapping("/history")
    public Result getSearchHistory(@RequestParam("userId") Long userId) {
        try {
            String key = RedisConstants.SEARCH_USER_HISTORY_KEY + userId;
            Set<Object> history = redisService.getCacheZSetReverseRange(key, 0, 9);
            return Result.ok(new ArrayList<>(history));
        } catch (Exception e) {
            log.error("获取用户历史搜索失败, userId: {}", userId, e);
            return Result.fail("获取历史搜索失败");
        }
    }

    /**
     * 清空用户社交搜索历史
     */
    @DeleteMapping("/history")
    public Result clearSearchHistory(@RequestParam("userId") Long userId) {
        try {
            String key = RedisConstants.SEARCH_USER_HISTORY_KEY + userId;
            redisService.deleteObject(key);
            return Result.ok("搜索历史已清空");
        } catch (Exception e) {
            log.error("清空用户搜索历史失败, userId: {}", userId, e);
            return Result.fail("清空搜索历史失败");
        }
    }
}
