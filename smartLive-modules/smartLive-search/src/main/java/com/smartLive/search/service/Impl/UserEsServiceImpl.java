package com.smartLive.search.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.service.IUserEsService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class UserEsServiceImpl implements IUserEsService {

    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Override
    public boolean insertOrUpdate(String indexName, String id, UserDoc data) throws IOException {
        // 1. 数据校验
        validateUser(data);
        // 2. 转换为JSON
        String json = objectMapper.writeValueAsString(data);
        // 3. 执行ES插入/更新
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        esClient.index(request, RequestOptions.DEFAULT);
        log.info("用户ES插入/更新成功：index={}, id={}", indexName, id);
        return true;
    }

    @Override
    public boolean batchInsert(String indexName, List<UserDoc> dataList, Function<UserDoc, String> idGenerator) throws IOException {
        if (dataList.isEmpty()) {
            log.warn("用户批量插入数据为空：index={}", indexName);
            return true;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (UserDoc data : dataList) {
            // 1. 逐条校验
            validateUser(data);
            // 2. 生成ID并添加到批量请求
            String id = idGenerator.apply(data);
            String json = objectMapper.writeValueAsString(data);
            bulkRequest.add(new IndexRequest(indexName).id(id).source(json, XContentType.JSON));
        }
        // 3. 执行批量操作
        esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("用户批量插入成功：index={}, 数量={}", indexName, dataList.size());
        return true;
    }

    @Override
    public boolean delete(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        esClient.delete(request, RequestOptions.DEFAULT);
        log.info("用户ES删除成功：index={}, id={}", indexName, id);
        return true;
    }
}