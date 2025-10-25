package com.smartLive.search.controller;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.follow.api.RemoteFollowService;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.domain.req.SearchHistoryDTO;
import com.smartLive.search.domain.req.SearchRecordDTO;
import com.smartLive.search.domain.res.SearchResult;
import com.smartLive.search.service.ISearchService;
import com.smartLive.search.utils.EsTool;
import com.smartLive.search.utils.ResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {
    @Autowired
    private ISearchService searchService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RemoteFollowService remoteFollowService;

    /**
     * 通用搜索
     */
    @GetMapping("/{indexName}")
    public ResponseEntity<Object> search(
            @PathVariable String indexName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            SearchResponse response = searchService.search(indexName, keyword, page, size);
            List<?> dataList = EsTool.convertSearchResult(indexName, response);
            dataList.forEach(item -> {
                if (item instanceof UserDoc) {
                    UserDoc user = (UserDoc) item;
                    user.setIsFollow((Boolean) remoteFollowService.isFollowed(user.getId()).getData());
                }
            });
            Map<String, Object> result = ResponseConverter.buildPageResult(response, dataList);
            return ResponseEntity.ok(SearchResult.success(result));
        } catch (Exception e) {
            log.error("搜索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索博客
     */
    @GetMapping("/blogs")
    public ResponseEntity<Object> searchBlogs( FilterSearchRequest request) {
        return search(EsIndexNameConstants.BLOG_INDEX_NAME, request.getKeyword(), request.getPage(), request.getSize());
    }

    /**
     * 搜索店铺
     */
    @PostMapping("/shops")
    public ResponseEntity<Object> searchShops( @RequestBody  FilterSearchRequest request) {
        try {
            SearchResponse response = searchService.searchShops(request);
            List<ShopDoc> shops = ResponseConverter.convertToShopList(response);
            Map<String, Object> result = ResponseConverter.buildPageResult(response, shops);
            return ResponseEntity.ok(SearchResult.success(result));
        } catch (Exception e) {
            log.error("搜索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("附近搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/users")
    public ResponseEntity<Object> searchUsers( FilterSearchRequest request) {
        return search(EsIndexNameConstants.USER_INDEX_NAME, request.getKeyword(), request.getPage(), request.getSize());
    }

    /**
     * 搜索优惠券
     */
    @PostMapping("/vouchers")
    public ResponseEntity<Object> searchVouchers(@RequestBody FilterSearchRequest request) {
            return searchWithFilter(EsIndexNameConstants.VOUCHER_INDEX_NAME, request);
    }

    /**
     * 带过滤条件的搜索
     */
    @PostMapping("/{indexName}/filter")
    public ResponseEntity<Object> searchWithFilter(
            @PathVariable String indexName,
            @RequestBody FilterSearchRequest request) {
        try {
            SearchResponse response = searchService.searchWithFilter(
                    indexName, request.getKeyword(), request.getFilters(),
                    request.getPage(), request.getSize());
            List<?> dataList = EsTool.convertSearchResult(indexName, response);
            Map<String, Object> result = ResponseConverter.buildPageResult(response, dataList);
            return ResponseEntity.ok(SearchResult.success(result));
        } catch (Exception e) {
            log.error("搜索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("搜索失败: " + e.getMessage()));
        }
    }

    // 添加搜索历史（去重）
    @PostMapping("/history")
    public Result addSearchHistory(@RequestBody SearchHistoryDTO dto) {
        try {
            searchService.insertSearchHistory(dto.getUserId(), dto.getKeyword());
            return Result.ok();
        } catch (Exception e) {
            log.error("添加搜索历史失败, userId: {}, keyword: {}",
                    dto.getUserId(), dto.getKeyword(), e);
            return Result.fail("添加搜索历史失败");
        }
    }
    // 获取历史搜索
    @GetMapping("/history")
    public Result getSearchHistory(@RequestParam Long userId) {
        try {
            String key = "search:history:" + userId;
            Set<String> history = stringRedisTemplate.opsForZSet()
                    .reverseRange(key, 0, 9);

            return Result.ok(new ArrayList<>(history));
        } catch (Exception e) {
            return Result.fail("获取历史搜索失败");
        }
    }
    // 清空用户搜索历史
    @DeleteMapping("/history")
    public Result clearSearchHistory(@RequestParam Long userId) {
        try {
            String key = "search:history:" + userId;
            stringRedisTemplate.delete(key);

            return Result.ok("搜索历史已清空");
        } catch (Exception e) {
            log.error("清空搜索历史失败, userId: {}", userId, e);
            return Result.fail("清空搜索历史失败");
        }
    }
    // 获取热门搜索
    @GetMapping("/hot")
    public Result getHotSearch() {
        try {
            String key = "search:hot:keywords";
            Set<ZSetOperations.TypedTuple<String>> hotKeywords = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, 0, 9);
            // 转换为前端需要的格式
            List<String> result = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : hotKeywords) {
                result.add(tuple.getValue());
            }
            return Result.ok(new ArrayList<>(result));
        } catch (Exception e) {
            return Result.fail("获取热门搜索失败");
        }
    }
    // 记录搜索（用于热门搜索统计）
    @PostMapping("/record")
    public Result recordSearch(@RequestBody SearchRecordDTO dto) {
        try {
            searchService.recordSearch(dto.getKeyword());
            return Result.ok();
        } catch (Exception e) {
            log.error("记录搜索失败, keyword: {}", dto.getKeyword(), e);
            return Result.fail("记录搜索失败");
        }
    }
}
