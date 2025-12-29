package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.service.rag.ICommentRagService;
import com.smartLive.comment.api.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CommentRagService implements ICommentRagService {
    private final VectorStore commentVectorStore;
    public CommentRagService(@Qualifier("commentVectorStore") VectorStore vectorStore) {
        this.commentVectorStore = vectorStore;
    }
    /**
     * 获取评论列表
     *
     * @param commentVO
     * @param userMessage
     * @return
     */
    @Override
    public List<CommentDTO> getComments(CommentDTO commentVO, String userMessage) {
        // 使用用户原始消息作为RAG查询
        String ragQuery = userMessage != null ? userMessage : "评价";

        // 调用RAG - 使用用户原始消息
        List<Document> results = commentVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ragQuery)  // 使用用户消息
                        .topK(5)
                        .filterExpression(buildFilterExpression(commentVO))
                        .build());
        log.info("🔍 RAG搜索结果：{}", results);
        if (results.isEmpty()){
            return null;
        }
        // 2. 将Document列表转为ShopVO列表
        List<CommentDTO> commentVOS = convertDocumentsToCommentVO(results);
        log.info("🔍 RAG搜索到 {} 个店铺", commentVOS);
        return commentVOS;
    }

    private String buildFilterExpression(CommentDTO commentVO) {

        List<String> filters = new ArrayList<>();
        if(commentVO.getSourceType() != null){
            filters.add("sourceType == " + commentVO.getSourceType());
        }
        if(commentVO.getSourceName() != null){
            filters.add(String.format("sourceName == '%s'", commentVO.getSourceName()));
        }
        if(commentVO.getSourceId() != null){
            filters.add("sourceId == " + commentVO.getSourceId());
        }
        return filters.isEmpty() ? "" : String.join(" && ", filters);
    }


    /**
     * 将Document列表转为CommentVO列表
     */
    private List<CommentDTO> convertDocumentsToCommentVO(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToCommentVO)
                .filter(Objects::nonNull)  // 过滤掉转换失败的
                .toList();
    }

    /**
     * 将单个Document转为CommentVO
     */
    private CommentDTO convertDocumentToCommentVO(Document document) {
        try {
            Map<String, Object> metadata = document.getMetadata();

            CommentDTO comment = new CommentDTO();
            // 从元数据中提取字段
            if (metadata.containsKey("id")) {
                comment.setId(Long.valueOf(metadata.get("id").toString()));
            }
            if (metadata.containsKey("userId")) {
                comment.setUserId(Long.valueOf(metadata.get("userId").toString()));
            }
            if (metadata.containsKey("sourceType")) {
                comment.setSourceType(Integer.valueOf(metadata.get("sourceType").toString()));
            }
            if (metadata.containsKey("sourceId")) {
                comment.setSourceId(Long.valueOf(metadata.get("sourceId").toString()));
            }
            if (metadata.containsKey("parentId")) {
                comment.setParentId(Long.valueOf(metadata.get("parentId").toString()));
            }
            if (metadata.containsKey("answerId")) {
                comment.setAnswerId(Long.valueOf(metadata.get("answerId").toString()));
            }
            if (metadata.containsKey("images")) {
                comment.setImages(metadata.get("images").toString());
            }
            if (metadata.containsKey("content")) {
                comment.setContent(metadata.get("content").toString());
            }
            if (metadata.containsKey("liked")) {
                comment.setLiked(Integer.valueOf(metadata.get("liked").toString()));
            }
            if (metadata.containsKey("status")) {
                comment.setStatus(Integer.valueOf(metadata.get("status").toString()));
            }
            if (metadata.containsKey("rating")) {
                comment.setRating(Integer.valueOf(metadata.get("rating").toString()));
            }
            if (metadata.containsKey("nickName")) {
                comment.setNickName(metadata.get("nickName").toString());
            }
            if (metadata.containsKey("userIcon")) {
                comment.setUserIcon(metadata.get("userIcon").toString());
            }
            if (metadata.containsKey("createTime")) {
                Object createTimeObj = metadata.get("createTime");
                if (createTimeObj instanceof Long) {
                    Date createTime = new Date((Long) createTimeObj);
                    comment.setCreateTime(createTime);
                }
            }

            return comment;
        } catch (Exception e) {
            log.warn("转换Document到CommentVO失败: {}", e.getMessage());
            return null;
        }
    }
    public List<Document> getCommentDocuments(CommentDTO commentVO) {
        // 使用用户原始消息作为RAG查询

        // 调用RAG - 使用用户原始消息
        List<Document> results = commentVectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression(buildFilterExpression(commentVO))
                        .build());
        log.info("🔍 RAG搜索结果：{}", results);
        if (results.isEmpty()){
            return null;
        }
        return results;
    }
}
