package com.smartLive.search.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
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
@Component
@Slf4j
public class BlogEsStrategy implements EsSyncStrategy {
    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取策略的类型
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.BLOG.getCode();
    }

    /**
     * 单条插入或更新
     *
     * @param indexName
     * @param id
     * @param data
     * @return
     * @throws IOException
     */
    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        BlogDoc doc = EsTool.convertToObject((Map) data, BlogDoc.class);
        // 1. 数据校验
        validateBlog(doc);
        // 2. 转换为JSON
        String json = objectMapper.writeValueAsString(data);
        // 3. 执行ES插入/更新
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
        if (response.status() == RestStatus.CREATED
                || response.status() == RestStatus.OK) {
            log.info("博客ES插入/更新成功：index={}, id={}, result={}",
                    indexName, id, response.getResult());
            UserResourceMessage userResourceMessage = UserResourceMessage
                    .builder()
                    .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                    .sourceType(GlobalBizTypeEnum.BLOG.getCode())
                    .sourceId(doc.getId())
                    .data(doc)
                    .build();
            updateUserResource(userResourceMessage);
            return true;
        }
        log.error("博客ES插入/更新失败：index={}, id={}, status={}, result={}",
                indexName, id, response.status(), response.getResult());
        return false;
    }

    /**
     * 批量插入
     *
     * @param indexName   ES索引名
     * @param dataList    实体列表
     * @return 是否成功
     */
    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        List<BlogDoc> docList = EsTool.convertList(dataList, BlogDoc.class);
        if (docList.isEmpty()) {
            log.warn("博客批量插入数据为空：index={}", indexName);
            return false;
        }
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (BlogDoc data : docList) {
                // 1. 逐条校验
                validateBlog(data);
                // 2. 生成ID并添加到批量请求
                String id = data.getId().toString();
                String json = objectMapper.writeValueAsString(data);
                bulkRequest.add(new IndexRequest(indexName).id(id).source(json, XContentType.JSON));
            }
            // 3. 执行批量操作
            BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                log.error("线程{}博客批量插入失败：index={}, 数量={}, failures={}",Thread.currentThread().getName(),indexName, dataList.size(), response.buildFailureMessage());
                return false;
            }else{
                log.info("线程{}博客批量插入成功：index={}, 数量={}",Thread.currentThread().getName(),indexName, dataList.size());
                return true;
            }
        }catch (Exception e){
            log.error("线程{}博客批量插入失败：index={}, 数量={}",Thread.currentThread().getName(),indexName, dataList.size(),e);
            return false;
        }
    }

    /**
     * 按ID删除
     *
     * @param indexName ES索引名
     * @param id        文档ID
     * @return 是否成功
     */
    @Override
    public boolean delete(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        DeleteResponse delete = esClient.delete(request, RequestOptions.DEFAULT);
        if (delete.status() != RestStatus.OK) {
            log.error("博客ES删除失败：index={}, id={}, status={}", indexName, id, delete.status());
            return false;
        }
        log.info("博客ES删除成功：index={}, id={}", indexName, id);
        return true;
    }

    /**
     * @param
     * @return
     */
    @Override
    public boolean insertUserResource(UserResourceMessage userResourceMessage) throws IOException {
        BlogDoc doc = EsTool.convertToObject((Map) userResourceMessage.getData(), BlogDoc.class);
        // 1. 数据校验
        validateBlog(doc);
        doc.setSourceType(userResourceMessage.getSourceType());
        doc.setSourceId(userResourceMessage.getSourceId());
        doc.setActionType(userResourceMessage.getActionType());
        // 2. 转换为JSON
        String json = objectMapper.writeValueAsString(doc);
        // 3. 执行ES插入/更新
        IndexRequest request = new IndexRequest(userResourceMessage.getIndexName())
                .id(userResourceMessage.getId())
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
        if (response.status() == RestStatus.CREATED
                || response.status() == RestStatus.OK) {
            log.info("博客ES插入/更新成功：index={}, id={}, result={}",
                    userResourceMessage.getIndexName(), userResourceMessage.getId(), response.getResult());
            return true;
        }
        log.error("博客ES插入/更新失败：index={}, id={}, status={}, result={}",
                userResourceMessage.getIndexName(), userResourceMessage.getId(), response.status(), response.getResult());
        return false;
    }


/**
 * 根据 sourceId 和 sourceType 批量更新
 * 场景：博主改了标题，ES里所有收藏了这篇博客的记录（不管属于哪个用户）都要同步更新标题
 */
    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        // 1. 校验关键条件
        if (msg.getSourceId() == null || msg.getSourceType() == null) {
            log.error("更新用户资源失败：sourceId 或 sourceType 为空{}",msg);
            return false;
        }

        // 2. 将消息数据转为 BlogDoc 对象
        // msg.getData() 是一个 Map 或者 JSON 字符串，根据你的实际情况转换
        BlogDoc newBlogData = (BlogDoc) msg.getData();

        if (newBlogData == null) {
            return false;
        }

        // 3. 准备 UpdateByQueryRequest
        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());

        // 4. 设置查询条件 (WHERE)
        // 逻辑：sourceId = 博客ID 且 sourceType = 博客类型
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        // 5. 准备参数 Map (SET 的值)
        // ★★★ 核心：这里做字段映射！把 BlogDoc 的字段映射到 ES 的字段 ★★★
        Map<String, Object> params = new HashMap<>();

        // 映射 1: blog.title -> es.title
        if (newBlogData.getTitle() != null) {
            params.put("newTitle", newBlogData.getTitle());
        }

        // 映射 2: blog.images -> es.imageUrl
        // (你的BlogDoc叫images，但收藏列表通常只需要一张封面图，所以ES里一般叫imageUrl)
        if (newBlogData.getImages() != null) {
            // 如果 images 是逗号分隔的 "url1,url2"，我们可能只取第一张作为封面
            String cover = newBlogData.getImages().split(",")[0];
            params.put("newImage", cover);
        }

        // 映射 3: blog.content -> es.subTitle
        // (收藏列表不需要展示几千字的正文，通常把 content 当作 subTitle 简介)
        if (newBlogData.getContent() != null) {
            // 截取前 50 个字作为简介，防止数据过大
            String summary = newBlogData.getContent().length() > 50
                    ? newBlogData.getContent().substring(0, 50)
                    : newBlogData.getContent();
            params.put("newSubTitle", summary);
        }

        // 6. 动态构建 Script 脚本
        // 只有当参数存在时，才生成对应的更新语句，防止把 ES 里的数据覆盖成 null
        StringBuilder scriptCode = new StringBuilder();

        if (params.containsKey("newTitle")) {
            scriptCode.append("ctx._source.title = params.newTitle;");
        }
        if (params.containsKey("newImage")) {
            // 注意：这里 ctx._source.imageUrl 必须是你 ES 索引里实际定义的字段名
            scriptCode.append("ctx._source.imageUrl = params.newImage;");
        }
        if (params.containsKey("newSubTitle")) {
            scriptCode.append("ctx._source.subTitle = params.newSubTitle;");
        }

        // 如果没有需要更新的字段，直接返回
        if (scriptCode.length() == 0) {
            log.info("没有需要更新的字段，跳过");
            return true;
        }

        request.setScript(new Script(
                ScriptType.INLINE,
                "painless",
                scriptCode.toString(),
                params
        ));

        // 7. 忽略并发冲突并执行
        request.setConflicts("proceed");
        try {
            BulkByScrollResponse response = esClient.updateByQuery(request, RequestOptions.DEFAULT);
            log.info("同步更新成功，影响条数：{}", response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("同步更新用户资源失败", e);
            return false;
        }
    }
    /**
     * 博客数据校验（特有的校验逻辑）
     */
    private void validateBlog(BlogDoc data) {
        if (data.getId() == null) {
            throw new IllegalArgumentException("博客ID不能为空");
        }
        if (data.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (data.getTitle() == null || data.getTitle().isEmpty()) {
            throw new IllegalArgumentException("博客标题不能为空");
        }
        if (data.getContent() == null || data.getContent().isEmpty()) {
            throw new IllegalArgumentException("博客内容不能为空");
        }
        if (data.getCreateTime() == null) {
            throw new IllegalArgumentException("博客创建时间不能为空");
        }
    }

}
