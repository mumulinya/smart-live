package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.BlogVO;

import java.util.List;

/**
 * 博客 RAG 服务接口。
 */
public interface IBlogRagService {

    /**
     * 获取店铺博客摘要。
     */
    String getShopBlogSummary(Long shopId, String userMessage, Integer limit);

    /**
     * 搜索博客。
     */
    List<BlogVO> searchBlogs(String query, Long shopId, Integer limit);

    /**
     * 按 ID 获取博客。
     */
    BlogVO getBlogById(Long blogId, Long shopId);
}