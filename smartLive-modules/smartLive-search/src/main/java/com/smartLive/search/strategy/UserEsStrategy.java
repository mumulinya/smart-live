package com.smartLive.search.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.search.domain.UserDoc;
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
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserEsStrategy implements EsSyncStrategy {
    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取策略的类型
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.USER.getCode();
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
        UserDoc doc = EsTool.convertToObject((Map) data, UserDoc.class);
        // 1. 数据校验
        validateUser(doc);
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
            return true;
        }
        log.error("用户ES插入/更新失败：index={}, id={}, status={}, result={}",
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
        List<UserDoc> docList = EsTool.convertList(dataList, UserDoc.class);
        if (dataList.isEmpty()) {
            log.warn("用户批量插入数据为空：index={}", indexName);
            return true;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (UserDoc data : docList) {
            // 1. 逐条校验
            validateUser(data);
            // 2. 生成ID并添加到批量请求
            String id = data.getId().toString();
            String json = objectMapper.writeValueAsString(data);
            bulkRequest.add(new IndexRequest(indexName).id(id).source(json, XContentType.JSON));
        }
        // 3. 执行批量操作
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (response.hasFailures()) {
            log.error("用户批量插入失败：index={}, failures={}", indexName, response.buildFailureMessage());
            return false;
        }else{
            log.info("用户批量插入成功：index={}, 数量={}", indexName, dataList.size());
            return true;
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
            log.error("用户ES删除失败：index={}, id={}, status={}", indexName, id, delete.status());
            return false;
        }
        log.info("用户ES删除成功：index={}, id={}", indexName, id);
        return true;
    }
    /**
     * 用户数据校验（特有的校验逻辑）
     */
    private void validateUser(UserDoc data) {
        if (data.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (data.getNickName() == null || data.getNickName().isEmpty()) {
            throw new IllegalArgumentException("用户昵称不能为空");
        }
    }
}
