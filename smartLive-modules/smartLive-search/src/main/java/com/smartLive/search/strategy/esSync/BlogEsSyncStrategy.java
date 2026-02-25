package com.smartLive.search.strategy.esSync;

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
@Component
@Slf4j
public class BlogEsSyncStrategy implements EsSyncStrategy {
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
        //删除用户资源
        deleteUserResourceBySource(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME, Long.valueOf(id), GlobalBizTypeEnum.BLOG.getCode());
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
     * 批量更新用户资源（当源博客/店铺信息修改时调用）
     * 涵盖字段：标题、封面图、简介、博主头像、博主昵称、点赞数
     */
    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        // 1. 校验关键条件
        if (msg.getSourceId() == null || msg.getSourceType() == null) {
            log.error("更新用户资源失败：sourceId 或 sourceType 为空");
            return false;
        }

        // 2. ★ 安全转换数据 (防止 ClassCastException)
        // 使用 ObjectMapper 或你自己的工具类将 Map 转为 BlogDoc
        BlogDoc newBlogData = null;
        try {
            if (msg.getData() instanceof BlogDoc) {
                newBlogData = (BlogDoc) msg.getData();
            } else {
                // 假设 msg.getData() 是 Map，转为对象
                newBlogData = objectMapper.convertValue(msg.getData(), BlogDoc.class);
            }
        } catch (Exception e) {
            log.error("数据转换失败", e);
            return false;
        }

        if (newBlogData == null) {
            return false;
        }

        // 3. 准备 Request
        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());

        // 4. 设置 WHERE 条件
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        // 5. ★★★ 核心：字段映射 (Java字段 -> ES参数 -> ES字段) ★★★
        Map<String, Object> params = new HashMap<>();

        // --- A. 基础内容映射 ---
        // 1. 标题
        if (newBlogData.getTitle() != null) {
            params.put("newTitle", newBlogData.getTitle());
        }

        // 2. 封面图 (只取第一张)
        if (newBlogData.getImages() != null) {
            String cover = newBlogData.getImages().split(",")[0];
            params.put("newImage", cover);
        }

        // 3. 简介 (截取前50字)
        if (newBlogData.getContent() != null) {
            String summary = newBlogData.getContent().length() > 50
                    ? newBlogData.getContent().substring(0, 50)
                    : newBlogData.getContent();
            params.put("newSubTitle", summary);
        }

        // --- B. 新增字段映射 (你要求的) ---
        // 4. 用户头像 (博主换头像了，收藏里的头像也要变)
        if (newBlogData.getIcon() != null) {
            params.put("newIcon", newBlogData.getIcon());
        }

        // 5. 用户昵称 (博主改名了)
        if (newBlogData.getName() != null) {
            params.put("newName", newBlogData.getName());
        }

        // 6. 点赞数 (同步最新的点赞数量)
        if (newBlogData.getLiked() != null) {
            params.put("newLiked", newBlogData.getLiked());
        }

        // 6. 动态构建 Script 脚本
        // 只有当 params 里有值时，才拼接对应的赋值语句
        StringBuilder scriptCode = new StringBuilder();

        if (params.containsKey("newTitle")) {
            scriptCode.append("ctx._source.title = params.newTitle;");
        }
        if (params.containsKey("newImage")) {
            scriptCode.append("ctx._source.imageUrl = params.newImage;"); // 假设ES里叫 imageUrl
        }
        if (params.containsKey("newSubTitle")) {
            scriptCode.append("ctx._source.subTitle = params.newSubTitle;");
        }
        if (params.containsKey("newIcon")) {
            scriptCode.append("ctx._source.icon = params.newIcon;"); // 假设ES里叫 icon
        }
        if (params.containsKey("newName")) {
            scriptCode.append("ctx._source.name = params.newName;"); // 假设ES里叫 name
        }
        if (params.containsKey("newLiked")) {
            scriptCode.append("ctx._source.liked = params.newLiked;"); // 假设ES里叫 liked
        }

        // 如果没有字段需要更新，直接跳过
        if (scriptCode.length() == 0) {
            log.info("没有检测到变化字段，跳过更新");
            return true;
        }

        request.setScript(new Script(
                ScriptType.INLINE,
                "painless",
                scriptCode.toString(),
                params
        ));

        // 7. 执行更新
        request.setConflicts("proceed");
        try {
            BulkByScrollResponse response = esClient.updateByQuery(request, RequestOptions.DEFAULT);
            log.info("同步更新成功，sourceId={}, 影响条数={}", msg.getSourceId(), response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("同步更新用户资源失败", e);
            return false;
        }
    }
    /**
     * 根据 sourceId 和 sourceType 批量删除
     * 场景：博客/店铺被删除了，需要把所有用户的收藏记录也删掉
     */
    @Override
    public boolean deleteUserResourceBySource(String indexName, Long sourceId, Integer sourceType) throws IOException {
        // 1. 构建 DeleteByQuery 请求
        DeleteByQueryRequest request = new DeleteByQueryRequest(indexName);

        // 2. 设置“WHERE”条件
        // 逻辑：删除所有 sourceId = ? AND sourceType = ? 的文档
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", sourceId))
                .must(QueryBuilders.termQuery("sourceType", sourceType)));

        // 3. 处理版本冲突（建议 proceed，防止因为某条数据正在被修改而导致整个删除任务中断）
        request.setConflicts("proceed");

        try {
            // 4. 执行删除
            BulkByScrollResponse response = esClient.deleteByQuery(request, RequestOptions.DEFAULT);

            // 5. 打印结果
            long deleted = response.getDeleted();
            log.info("源资源被删，已级联删除用户收藏数据：sourceId={}, 数量={}", sourceId, deleted);
            return true;
        } catch (Exception e) {
            log.error("级联删除失败：sourceId={}", sourceId, e);
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
