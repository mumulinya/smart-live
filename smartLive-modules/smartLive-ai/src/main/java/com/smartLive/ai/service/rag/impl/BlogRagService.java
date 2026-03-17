package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.BlogVO;
import com.smartLive.ai.service.rag.IBlogRagService;
import com.smartLive.ai.utils.RagMetadataValueUtils;
import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.blog.api.RemoteBlogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 博客 RAG 服务实现类。
 */
@Service
@Slf4j
public class BlogRagService implements IBlogRagService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final VectorStore blogVectorStore;
    private final RemoteBlogService remoteBlogService;

    /**
     * 构造博客 RAG 服务实现类。
     */
    public BlogRagService(@Qualifier("blogVectorStore") VectorStore vectorStore,
                          RemoteBlogService remoteBlogService) {
        this.blogVectorStore = vectorStore;
        this.remoteBlogService = remoteBlogService;
    }

    /**
     * 获取店铺博客摘要。
     */
    @Override
    public String getShopBlogSummary(Long shopId, String userMessage, Integer limit) {
        List<BlogVO> blogs = searchBlogs(userMessage, shopId, limit);
        if (blogs.isEmpty()) {
            return "No blog data.";
        }

        StringBuilder context = new StringBuilder("Blog reference:\n");
        for (BlogVO blog : blogs) {
            context.append("- Title: ").append(defaultText(blog.getTitle(), "Untitled"));
            if (StringUtils.hasText(blog.getName())) {
                context.append("; Author: ").append(blog.getName());
            }
            if (blog.getLiked() != null) {
                context.append("; Likes: ").append(blog.getLiked());
            }
            if (blog.getComments() != null) {
                context.append("; Comments: ").append(blog.getComments());
            }
            if (blog.getCreateTime() != null) {
                context.append("; Date: ")
                        .append(DATE_FORMAT.format(blog.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));
            }
            String excerpt = abbreviate(cleanContent(blog.getContent()), 100);
            if (StringUtils.hasText(excerpt)) {
                context.append("; Content: ").append(excerpt);
            }
            context.append('\n');
        }
        return context.toString();
    }

    /**
     * 搜索博客。
     */
    @Override
    public List<BlogVO> searchBlogs(String query, Long shopId, Integer limit) {
        String safeQuery = StringUtils.hasText(query) ? query : "store visit recommendation taste environment service";
        String filter = buildShopFilter(shopId);
        List<Document> docs = searchBlogDocs(safeQuery, filter, normalizeLimit(limit));
        return convertDocumentsToBlogVo(docs);
    }

    /**
     * 按 ID 获取博客。
     */
    @Override
    public BlogVO getBlogById(Long blogId, Long shopId) {
        if (blogId == null) {
            return null;
        }
        BlogDTO blogDTO = remoteBlogService.getBlogById(blogId);
        if (blogDTO == null) {
            return null;
        }
        if (shopId != null && !shopId.equals(blogDTO.getShopId())) {
            return null;
        }
        return convertBlogDtoToBlogVo(blogDTO);
    }

    /**
     * 搜索博客文档。
     */
    private List<Document> searchBlogDocs(String query, String filterExpression, int topK) {
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);
            if (StringUtils.hasText(filterExpression)) {
                builder.filterExpression(filterExpression);
            }
            List<Document> documents = blogVectorStore.similaritySearch(builder.build());
            return documents == null ? new ArrayList<>() : documents;
        } catch (Exception ex) {
            log.warn("Blog vector search failed, query={}, filter={}, error={}",
                    query, filterExpression, ex.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 转换博客数据传输对象博客视图对象。
     */
    private BlogVO convertBlogDtoToBlogVo(BlogDTO blogDTO) {
        if (blogDTO == null) {
            return null;
        }
        BlogVO blogVO = new BlogVO();
        blogVO.setId(blogDTO.getId());
        blogVO.setShopId(blogDTO.getShopId());
        blogVO.setUserId(blogDTO.getUserId());
        blogVO.setTitle(blogDTO.getTitle());
        blogVO.setImages(blogDTO.getImages());
        blogVO.setContent(blogDTO.getContent());
        blogVO.setLiked(blogDTO.getLiked());
        blogVO.setComments(blogDTO.getComments());
        blogVO.setIcon(blogDTO.getIcon());
        blogVO.setName(blogDTO.getName());
        blogVO.setCreateTime(blogDTO.getCreateTime());
        return blogVO;
    }

    /**
     * 将文档列表转换为博客视图对象。
     */
    private List<BlogVO> convertDocumentsToBlogVo(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<BlogVO> list = new ArrayList<>(documents.size());
        for (Document document : documents) {
            BlogVO blogVO = convertDocumentToBlogVo(document);
            if (blogVO != null) {
                list.add(blogVO);
            }
        }
        return list;
    }

    /**
     * 将文档转换为博客视图对象。
     */
    private BlogVO convertDocumentToBlogVo(Document document) {
        if (document == null) {
            return null;
        }
        try {
            Map<String, Object> metadata = document.getMetadata();
            if (metadata == null) {
                metadata = Map.of();
            }

            BlogVO blogVO = new BlogVO();
            blogVO.setId(RagMetadataValueUtils.toLong(metadata.get("id")));
            blogVO.setShopId(RagMetadataValueUtils.toLong(metadata.get("shopId")));
            blogVO.setTypeId(RagMetadataValueUtils.toLong(metadata.get("typeId")));
            blogVO.setUserId(RagMetadataValueUtils.toLong(metadata.get("userId")));
            blogVO.setTitle(RagMetadataValueUtils.toStringValue(metadata.get("title")));
            blogVO.setImages(RagMetadataValueUtils.toStringValue(metadata.get("images")));
            blogVO.setLiked(RagMetadataValueUtils.toInteger(metadata.get("liked")));
            blogVO.setComments(RagMetadataValueUtils.toInteger(metadata.get("comments")));
            blogVO.setIcon(RagMetadataValueUtils.toStringValue(metadata.get("icon")));
            blogVO.setName(RagMetadataValueUtils.toStringValue(metadata.get("name")));

            String content = RagMetadataValueUtils.toStringValue(metadata.get("content"));
            blogVO.setContent(StringUtils.hasText(content) ? content : document.getText());

            Long createTime = RagMetadataValueUtils.toLong(metadata.get("createTime"));
            if (createTime != null) {
                blogVO.setCreateTime(new Date(createTime));
            }
            return blogVO;
        } catch (Exception ex) {
            log.warn("Convert blog vector document failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 构建店铺过滤条件。
     */
    private String buildShopFilter(Long shopId) {
        return shopId == null ? "" : "shopId == " + shopId;
    }

    /**
     * 规范化限制数量。
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 获取字符串结果。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    /**
     * 获取字符串结果。
     */
    private String cleanContent(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    /**
     * 获取字符串结果。
     */
    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}