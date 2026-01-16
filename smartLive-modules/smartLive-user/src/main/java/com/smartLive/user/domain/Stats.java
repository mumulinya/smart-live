package com.smartLive.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Stats {
    //博客数
    private Integer blogCount;
    //关注数
    private Integer followCount;
    //粉丝数
    private Integer fansCount;
    //获赞数
    private Integer likeCount;
    //评论数
    private Integer commentCount;
    //订单数
    private Integer orderCount;
    //关注店铺数
    private Integer followShopCount;
    //共同关注数
    private Integer commonFollowCount;
    //博客喜欢数
    private Integer blogLikeCount;
    //博客收藏数
    private Integer blogStarCount;
}
