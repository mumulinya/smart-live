package com.smartLive.index.domain;

import lombok.Data;

/**
 * 最新博客 VO（匹配前端 recentBlogs 单个对象）
 */
@Data
public class RecentBlogVO {
    private Long id;            // 博客ID
    private String title;       // 标题
    private String summary;     // 摘要
    private Integer status;     // 状态：1已发布/0草稿
    private String createTime;  // 创建时间（格式：YYYY-MM-DD HH:mm:ss）
}