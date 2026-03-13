package com.smartLive.search.strategy.esSync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.utils.EsTool;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 博客/笔记数据同步策略实现
 * 负责将探店笔记同步至 ES 博客主索引，并维护用户资源宽表（UserResource）中的博客快照。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component
@Slf4j
public class BlogEsSyncStrategy implements EsSyncStrategy {
    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.BLOG.getCode();
    }

    /**
     * 同步单条博客数据
     * 成功后会触发用户资源宽表（收藏/点赞流）中对应博客信息的级联更新。
     */
    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        BlogDoc doc = EsTool.convertToObject((Map) data, BlogDoc.class);
        validateBlog(doc);
        
        String json = objectMapper.writeValueAsString(data);
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
        
        if (response.status() == RestStatus.CREATED || response.status() == RestStatus.OK) {
            log.info("博客索引发布成功: id={}", id);
            // 级联更新用户资源库中的该博文快照（如标题、点赞数变更）
            UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                    .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                    .sourceType(GlobalBizTypeEnum.BLOG.getCode())
                    .sourceId(doc.getId())
                    .data(doc)
                    .build();
            updateUserResource(userResourceMessage);
            return true;
        }
        return false;
    }

    /**
     * 批量同步博客数据
     * 利用 Bulk API 提升初始化或全量导入时的性能。
     */
    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        List<BlogDoc> docList = EsTool.convertList(dataList, BlogDoc.class);
        if (docList.isEmpty()) return true;

        BulkRequest bulkRequest = new BulkRequest();
        for (BlogDoc data : docList) {
            validateBlog(data);
            String json = objectMapper.writeValueAsString(data);
            bulkRequest.add(new IndexRequest(indexName).id(data.getId().toString()).source(json, XContentType.JSON));
        }
        
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (response.hasFailures()) {
            log.error("博客批量同步失败: {}", response.buildFailureMessage());
            return false;
        }
        return true;
    }

    /**
     * 删除博客及关联资源
     * 物理删除博客索引后，会通过 deleteByQuery 将用户资源表中心关联的所有收藏记录一并清除。
     */
    @Override
    public boolean delete(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        DeleteResponse delete = esClient.delete(request, RequestOptions.DEFAULT);
        if (delete.status() == RestStatus.OK) {
            log.info("博客物理删除成功: id={}", id);
            // 级联清除该博客关联的所有“收藏/点赞”记录文档
            deleteUserResourceBySource(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME, Long.valueOf(id), GlobalBizTypeEnum.BLOG.getCode());
            return true;
        }
        return false;
    }

    /**
     * 初始化/重写用户资源记录
     * 用于用户初次触发收藏、点赞等动作时，将博客详情作为快照注入资源索引。
     */
    @Override
    public boolean insertUserResource(UserResourceMessage userResourceMessage) throws IOException {
        BlogDoc doc = EsTool.convertToObject((Map) userResourceMessage.getData(), BlogDoc.class);
        validateBlog(doc);
        
        doc.setSourceType(userResourceMessage.getSourceType());
        doc.setSourceId(userResourceMessage.getSourceId());
        doc.setActionType(userResourceMessage.getActionType());
        
        String json = objectMapper.writeValueAsString(doc);
        IndexRequest request = new IndexRequest(userResourceMessage.getIndexName())
                .id(userResourceMessage.getId())
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
        return response.status() == RestStatus.CREATED || response.status() == RestStatus.OK;
    }

    /**
     * 级联更新用户资源库中的博客字段
     * 采用 UpdateByQuery + Painless 脚本，实现无锁化批量更新。
     * 同步字段：标题、封面、内容摘要、博主头像、昵称、点赞数。
     */
    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        if (msg.getSourceId() == null || msg.getSourceType() == null) return false;

        BlogDoc newBlogData = objectMapper.convertValue(msg.getData(), BlogDoc.class);
        if (newBlogData == null) return false;

        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        Map<String, Object> params = new HashMap<>();
        if (newBlogData.getTitle() != null) params.put("newTitle", newBlogData.getTitle());
        if (newBlogData.getImages() != null) params.put("newImage", newBlogData.getImages().split(",")[0]);
        if (newBlogData.getContent() != null) {
            String summary = newBlogData.getContent().length() > 50 ? newBlogData.getContent().substring(0, 50) : newBlogData.getContent();
            params.put("newSubTitle", summary);
        }
        if (newBlogData.getIcon() != null) params.put("newIcon", newBlogData.getIcon());
        if (newBlogData.getName() != null) params.put("newName", newBlogData.getName());
        if (newBlogData.getLiked() != null) params.put("newLiked", newBlogData.getLiked());

        if (params.isEmpty()) return true;

        StringBuilder scriptCode = new StringBuilder();
        if (params.containsKey("newTitle")) scriptCode.append("ctx._source.title = params.newTitle;");
        if (params.containsKey("newImage")) scriptCode.append("ctx._source.imageUrl = params.newImage;");
        if (params.containsKey("newSubTitle")) scriptCode.append("ctx._source.subTitle = params.newSubTitle;");
        if (params.containsKey("newIcon")) scriptCode.append("ctx._source.icon = params.newIcon;");
        if (params.containsKey("newName")) scriptCode.append("ctx._source.name = params.newName;");
        if (params.containsKey("newLiked")) scriptCode.append("ctx._source.liked = params.newLiked;");

        request.setScript(new Script(ScriptType.INLINE, "painless", scriptCode.toString(), params));
        request.setConflicts("proceed");
        
        try {
            BulkByScrollResponse response = esClient.updateByQuery(request, RequestOptions.DEFAULT);
            log.info("级联同步更新完成，博客ID: {}, 影响记录: {}", msg.getSourceId(), response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("级联同步失败", e);
            return false;
        }
    }

    /**
     * 级联删除用户关联资源
     */
    @Override
    public boolean deleteUserResourceBySource(String indexName, Long sourceId, Integer sourceType) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(indexName);
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", sourceId))
                .must(QueryBuilders.termQuery("sourceType", sourceType)));
        request.setConflicts("proceed");
        try {
            BulkByScrollResponse response = esClient.deleteByQuery(request, RequestOptions.DEFAULT);
            log.info("资源物理清除，同步移除用户关联数据: sourceId={}, count={}", sourceId, response.getDeleted());
            return true;
        } catch (Exception e) {
            log.error("级联物理删除失败", e);
            return false;
        }
    }

    /**
     * 博客同步前置校验
     */
    private void validateBlog(BlogDoc data) {
        if (data.getId() == null) throw new IllegalArgumentException("博客ID不能为空");
        if (data.getTitle() == null) throw new IllegalArgumentException("博客标题不能为空");
        if (data.getCreateTime() == null) throw new IllegalArgumentException("发布时间不能为空");
    }
}
