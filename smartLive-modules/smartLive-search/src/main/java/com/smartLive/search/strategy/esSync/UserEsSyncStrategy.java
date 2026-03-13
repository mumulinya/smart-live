package com.smartLive.search.strategy.esSync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
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

/**
 * 用户基础信息同步策略实现
 * 负责将用户昵称、头像、城市及简介等信息同步至 ES 用户主索引。
 * 用于实现跨模块的用户昵称模糊搜索。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component
@Slf4j
public class UserEsSyncStrategy implements EsSyncStrategy {
    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.USER.getCode();
    }

    /**
     * 同步单条用户信息
     */
    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        UserDoc doc = EsTool.convertToObject((Map) data, UserDoc.class);
        validateUser(doc);
        
        String json = objectMapper.writeValueAsString(data);
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
        
        if (response.status() == RestStatus.CREATED || response.status() == RestStatus.OK) {
            log.info("用户信息索引已刷新: id={}", id);
            return true;
        }
        return false;
    }

    /**
     * 批量导入用户信息
     */
    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        List<UserDoc> docList = EsTool.convertList(dataList, UserDoc.class);
        if (dataList.isEmpty()) return true;
        
        BulkRequest bulkRequest = new BulkRequest();
        for (UserDoc data : docList) {
            validateUser(data);
            String json = objectMapper.writeValueAsString(data);
            bulkRequest.add(new IndexRequest(indexName).id(data.getId().toString()).source(json, XContentType.JSON));
        }
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !response.hasFailures();
    }

    /**
     * 注销用户
     */
    @Override
    public boolean delete(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        DeleteResponse delete = esClient.delete(request, RequestOptions.DEFAULT);
        return delete.status() == RestStatus.OK;
    }

    private void validateUser(UserDoc data) {
        if (data.getId() == null) throw new IllegalArgumentException("用户ID必填");
        if (data.getNickName() == null) throw new IllegalArgumentException("用户昵称必填");
    }
}
