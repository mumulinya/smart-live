package com.smartLive.search;

import com.smartLive.search.service.Impl.SimpleSearchService;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.*;

@SpringBootTest
public class SimpleSearchTest {

    @Autowired
    private SimpleSearchService simpleSearchService;

    /**
     * 简单搜索测试
     */
    @Test
    public void testSimpleSearch() throws IOException {
        // 搜索博客
        SearchResponse blogResponse = simpleSearchService.search("blog_index", "美食", 1, 10);
        System.out.println("博客搜索结果: " + blogResponse.getHits().getTotalHits().value);

        // 搜索用户
        SearchResponse userResponse = simpleSearchService.search("user_index", "北京", 1, 10);
        System.out.println("用户搜索结果: " + userResponse.getHits().getTotalHits().value);
        System.out.println("数据为" + userResponse.getHits().getHits()[0].getSourceAsString());
        // 搜索用户
        SearchResponse shopResponse = simpleSearchService.search("shop_index", "", 1, 10);
        System.out.println("店铺搜索结果: " + shopResponse.getHits().getTotalHits().value);
        System.out.println("数据为" + shopResponse.getHits().getHits()[0].getSourceAsString());
    }

    /**
     * 附近店铺搜索测试
     */
    @Test
    public void testNearbyShopSearch() throws IOException {
        // 搜索北京天安门5公里内的餐厅
        SearchResponse response = simpleSearchService.searchNearbyShops(
                116.3974, 39.9093, "5km", "餐厅", 1, 20);
        System.out.println("附近店铺结果: " + response.getHits().getTotalHits().value);
    }

    /**
     * 带过滤的搜索测试
     */
    @Test
    public void testSearchWithFilter() throws IOException {
        // 搜索优惠券：类型为普通券且状态为上架
        Map<String, Object> filters = new HashMap<>();
        filters.put("type", 0);
        filters.put("status", 1);

        SearchResponse response = simpleSearchService.searchWithFilter(
                "voucher_index", "折扣", filters, 1, 10);
        System.out.println("过滤搜索结果: " + response.getHits().getTotalHits().value);
    }

    class Solution {
        public int trap(int[] height) {
            int are=0;
            for (int i = 0; i < height.length;i++) {
                for (int j=i+1;j<height.length;j++){
                    if(height[i]-height[j]>0&&j<height.length-1){
                        are+=height[i]-height[j];
                    }else{
                        i=j;
                        break;
                    }
                }
            }
            return are;
        }
    }


}