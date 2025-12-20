package com.smartLive.interaction.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ResourceVO {
    private Long id;
    private Integer type;      // BLOG, GOODS, COUPON
    
    private String title;      // 博客标题 / 商品名 / 代金券名
    private String cover;      // 博客封面 / 商品主图
    private String content;    // 博客摘要 / 商品价格 / 代金券面额 (差异化展示)
    private Boolean isLike;
    private String userName;
    private String userAvatar;
    //销量
    private Integer sales;
    //距离
    private String distance;
    //原价
    private String originalPrice;
    //现价
    private String presentPrice;
    //店铺名
    private String shopName;
}