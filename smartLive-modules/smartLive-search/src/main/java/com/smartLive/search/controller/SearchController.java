package com.smartLive.search.controller;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.req.FilterSearchRequest;
import com.smartLive.search.domain.res.SearchResult;
import com.smartLive.search.service.ISearchService;
import com.smartLive.search.utils.EsTool;
import com.smartLive.search.utils.ResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;


/**
 * 全栈搜索控制器
 * 提供基于 Elasticsearch 的多维搜索接口，包括通用搜索、业务专项搜索（博客、店铺、用户、商品）以及搜索历史/热搜管理。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {
    @Autowired
    private ISearchService searchService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RemoteFollowService remoteFollowService;

    /**
     * 通用关键词搜索
     * 根据索引名称进行跨字段匹配搜索。对于用户搜索，会自动关联当前登录用户的关注状态。
     * 
     * @param indexName 索引名称 (如 blogs, users)
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 统一封装的分页结果转换对象
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
            // 针对用户搜索，增强关注状态数据
            dataList.forEach(item -> {
                if (item instanceof UserDoc) {
                    UserDoc user = (UserDoc) item;
                    FollowDTO followDTO = new FollowDTO();
                    followDTO.setUserId(Long.valueOf(user.getId()));
                    followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
                    followDTO.setSourceId(Long.valueOf(user.getId()));
                    Boolean isFollow = remoteFollowService.isFollowed(followDTO);
                        user.setIsFollow(isFollow);
                }
            });
            Map<String, Object> result = ResponseConverter.buildPageResult(response, dataList);
            return ResponseEntity.ok(SearchResult.success(result));
        } catch (Exception e) {
            log.error("通用搜索执行异常: {}", e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 博客专项搜索
     * 支持多条件过滤（如分类、标签等）的博客搜索。
     */
    @PostMapping("/blogs")
    public ResponseEntity<Object> searchBlogs(@RequestBody  FilterSearchRequest request) {
        return searchWithFilter(EsIndexNameConstants.BLOG_INDEX_NAME, request);    }

    /**
     * 周边店铺搜索
     * 集成了地理位置 LBS 过滤与多维度排序（距离优先、评分优先、价格优先）。
     * 
     * @param request 包含坐标（lat/lon）、距离范围及排序规则的请求体
     * @return 格式化后的店铺列表，包含距离转换信息
     */
    @PostMapping("/shops")
    public ResponseEntity<Object> searchShops(@RequestBody  FilterSearchRequest request) {
        try {
            SearchResponse response = searchService.searchShops(request);
            List<ShopDoc> shops = ResponseConverter.convertToShopList(response,request);
            Map<String, Object> result = ResponseConverter.buildPageResult(response, shops);
            return ResponseEntity.ok(SearchResult.success(result));
        } catch (Exception e) {
            log.error("店铺周边搜索失败: {}", e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("附近搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 用户专项搜索
     */
    @GetMapping("/users")
    public ResponseEntity<Object> searchUsers(FilterSearchRequest request) {
        return search(EsIndexNameConstants.USER_INDEX_NAME, request.getKeyword(), request.getPage(), request.getSize());
    }

    /**
     * 商品/优惠券专项搜索
     */
    @PostMapping("/products")
    public ResponseEntity<Object> searchVouchers(@RequestBody FilterSearchRequest request) {
            return searchWithFilter(EsIndexNameConstants.PRODUCT_INDEX_NAME, request);
    }

    /**
     * 高级过滤搜索接口
     * 支持动态 Map 传参，实现对 ES 索引字段的精准 Filter 过滤。
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
            log.error("过滤搜索执行异常: index={}, error={}", indexName, e.getMessage());
            return ResponseEntity.status(500).body(SearchResult.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 持久化搜索历史
     * 使用 Redis ZSet 存储，按时间戳排序，实现自动去重及固定窗口（10条）展示。
     */
    @PostMapping("/history")
    public Result addSearchHistory(@RequestParam("userId") Long userId, @RequestParam("keyword") String keyword) {
        try {
            searchService.insertSearchHistory(userId, keyword);
            return Result.ok();
        } catch (Exception e) {
            log.error("存储历史搜索失败, userId: {}, keyword: {}", userId, keyword, e);
            return Result.fail("添加搜索历史失败");
        }
    }

    /**
     * 获取用户近期的搜索词
     */
    @GetMapping("/history")
    public Result getSearchHistory(@RequestParam("userId") Long userId) {
        try {
            String key = RedisConstants.SEARCH_INDEX_HISTORY_KEY + userId;
            Set<Object> history = redisService.getCacheZSetReverseRange(key, 0, 9);
            return Result.ok(new ArrayList<>(history));
        } catch (Exception e) {
            log.error("读取历史搜索失败, userId: {}", userId, e);
            return Result.fail("获取历史搜索失败");
        }
    }

    /**
     * 清空个人搜索历史记录
     */
    @DeleteMapping("/history")
    public Result clearSearchHistory(@RequestParam("userId") Long userId) {
        try {
            String key = RedisConstants.SEARCH_INDEX_HISTORY_KEY + userId;
            redisService.deleteObject(key);
            return Result.ok("搜索历史已清空");
        } catch (Exception e) {
            log.error("删除历史记录异常, userId: {}", userId, e);
            return Result.fail("清空搜索历史失败");
        }
    }

    /**
     * 获取全站热门搜索关键词
     * 从 Redis 热搜榜单中提取前 10 名。
     */
    @GetMapping("/hot")
    public Result getHotSearch() {
        try {
            String key = RedisConstants.SEARCH_HOT_KEYWORDS;
            Set<ZSetOperations.TypedTuple<String>> hotKeywords = redisService.getCacheZSetReverseRangeWithScores(key, 0, 9);
            List<String> result = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : hotKeywords) {
                result.add(tuple.getValue());
            }
            return Result.ok(new ArrayList<>(result));
        } catch (Exception e) {
            log.error("获取热搜榜单失败", e);
            return Result.fail("获取热门搜索失败");
        }
    }

    /**
     * 异步上报搜索关键词（用于热度统计）
     */
    @PostMapping("/record")
    public Result recordSearch(@RequestParam("keyword") String keyword) {
        try {
            searchService.recordSearch(keyword);
            return Result.ok();
        } catch (Exception e) {
            log.error("上报热搜数据失败, keyword: {}", keyword, e);
            return Result.fail("记录搜索失败");
        }
    }
}
