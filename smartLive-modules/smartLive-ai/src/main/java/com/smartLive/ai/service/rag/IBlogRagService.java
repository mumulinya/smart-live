package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.BlogVO;

import java.util.List;

public interface IBlogRagService {

    String getShopBlogSummary(Long shopId, String userMessage, Integer limit);

    List<BlogVO> searchBlogs(String query, Long shopId, Integer limit);

    BlogVO getBlogById(Long blogId, Long shopId);
}