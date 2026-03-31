package com.smartLive.ai.tools;

import com.smartLive.ai.entity.vo.BlogVO;
import com.smartLive.ai.service.rag.IBlogRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 博客工具集。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlogTools {

    private final IBlogRagService blogRagService;

    /**
     * 获取店铺博客摘要。
     */
    @Tool(description = "Get a blog summary for a specific shop.")
    public String getShopBlogSummary(
            @ToolParam(description = "Shop id.", required = false)
            Long shopId,
            @ToolParam(description = "Original user message.", required = false)
            String userMessage,
            @ToolParam(description = "Maximum number of blogs to summarize.", required = false)
            Integer limit,
            ToolContext toolContext
    ) {
        log.info("Calling getShopBlogSummary | shopId={}, limit={}", shopId, limit);
        return blogRagService.getShopBlogSummary(shopId, userMessage, limit);
    }

    /**
     * 搜索店铺博客。
     */
    @Tool(description = "Search blog notes for a specific shop.")
    public List<BlogVO> searchShopBlogs(
            @ToolParam(description = "Shop id.", required = false)
            Long shopId,
            @ToolParam(description = "Original user message.", required = false)
            String userMessage,
            @ToolParam(description = "Maximum number of blogs to return.", required = false)
            Integer limit,
            ToolContext toolContext
    ) {
        log.info("Calling searchShopBlogs | shopId={}, limit={}", shopId, limit);
        return blogRagService.searchBlogs(userMessage, shopId, limit);
    }
}
