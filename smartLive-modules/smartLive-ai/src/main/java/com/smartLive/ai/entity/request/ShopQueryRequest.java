package com.smartLive.ai.entity.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ShopQueryRequest {
    private String query;                   // 搜索关键词
    private String location;                // 位置描述：朝阳区、国贸等
    
    // 地理位置
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer radius;                 // 搜索半径（米）
    
    // 筛选条件
    private String type;                    // 店铺类型
    private String category;                // 分类
    private BigDecimal minRating;           // 最低评分
    private BigDecimal maxPrice;            // 最高价格
    private BigDecimal minPrice;            // 最低价格
    private String features;                // 特色要求：包间,wifi等
    private String tags;                    // 标签筛选
    
    // 排序方式
    private String sortBy = "comprehensive"; // comprehensive, distance, rating, price
    private String sortOrder = "desc";      // asc, desc
    
    // 分页
    private Integer pageSize = 20;
    private Integer pageNum = 1;
    
    // 业务标识
    private String searchId;                // 搜索ID，用于追踪
    private String source;                  // 来源：app, web, mini-program
    
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
    
    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }
    
    public boolean hasRatingFilter() {
        return minRating != null;
    }
}