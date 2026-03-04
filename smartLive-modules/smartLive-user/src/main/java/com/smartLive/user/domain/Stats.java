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
    //获赞数
    private Integer likeCount;
    //共同关注数
    private Integer commonFollowCount;
    //博客喜欢数
    private Integer blogLikeCount;
    //博客收藏数
    private Integer blogStarCount;
}
