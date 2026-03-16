package com.smartLive.search.strategy.esSync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.search.domain.ShopDoc;
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
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ShopEsSyncStrategy implements EsSyncStrategy {

    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.SHOP.getCode();
    }

    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        ShopDoc doc = EsTool.convertToObject((Map) data, ShopDoc.class);
        validateShop(doc);

        String json = objectMapper.writeValueAsString(EsTool.convertToJsonMap(doc));
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);

        if (response.status() == RestStatus.CREATED || response.status() == RestStatus.OK) {
            log.info("shop index sync success: id={}", id);
            UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                    .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                    .sourceType(GlobalBizTypeEnum.SHOP.getCode())
                    .sourceId(doc.getId())
                    .data(doc)
                    .build();
            updateUserResource(userResourceMessage);
            return true;
        }

        log.error("shop index sync failed: index={}, id={}, status={}", indexName, id, response.status());
        return false;
    }

    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        List<ShopDoc> docList = EsTool.convertList(dataList, ShopDoc.class);
        if (docList.isEmpty()) {
            return true;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (ShopDoc data : docList) {
            validateShop(data);
            String id = data.getId().toString();
            String json = objectMapper.writeValueAsString(EsTool.convertToJsonMap(data));
            bulkRequest.add(new IndexRequest(indexName).id(id).source(json, XContentType.JSON));
        }

        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (response.hasFailures()) {
            log.error("shop batch index sync failed: index={}, failures={}", indexName, response.buildFailureMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean delete(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        DeleteResponse delete = esClient.delete(request, RequestOptions.DEFAULT);
        if (delete.status() == RestStatus.OK) {
            deleteUserResourceBySource(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME, Long.valueOf(id), GlobalBizTypeEnum.SHOP.getCode());
            return true;
        }
        return false;
    }

    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        if (msg.getSourceId() == null || msg.getSourceType() == null) {
            return false;
        }

        ShopDoc shop = objectMapper.convertValue(msg.getData(), ShopDoc.class);
        if (shop == null) {
            return false;
        }

        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        Map<String, Object> params = new HashMap<>();
        if (shop.getName() != null) {
            params.put("title", shop.getName());
        }
        if (shop.getImages() != null && !shop.getImages().isEmpty()) {
            params.put("imageUrl", shop.getImages().split(",")[0]);
        }
        if (shop.getArea() != null) {
            params.put("subTitle", shop.getArea());
        } else if (shop.getAddress() != null) {
            params.put("subTitle", shop.getAddress());
        }
        if (shop.getScore() != null) {
            params.put("score", shop.getScore());
        }
        if (shop.getSold() != null) {
            params.put("sold", shop.getSold());
        }
        if (shop.getAvgPrice() != null) {
            params.put("avgPrice", shop.getAvgPrice());
        }
        if (shop.getLocation() != null) {
            params.put("location", shop.getLocation());
        }

        try {
            params.put("payload", objectMapper.writeValueAsString(shop));
        } catch (Exception ignored) {
        }

        if (params.isEmpty()) {
            return true;
        }

        String loopScript = "for (entry in params.entrySet()) { ctx._source[entry.getKey()] = entry.getValue(); }";
        request.setScript(new Script(ScriptType.INLINE, "painless", loopScript, params));
        request.setConflicts("proceed");

        try {
            BulkByScrollResponse response = esClient.updateByQuery(request, RequestOptions.DEFAULT);
            log.info("shop user-resource sync success: id={}, count={}", msg.getSourceId(), response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("shop user-resource sync failed", e);
            return false;
        }
    }

    @Override
    public boolean deleteUserResourceBySource(String indexName, Long sourceId, Integer sourceType) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(indexName);
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", sourceId))
                .must(QueryBuilders.termQuery("sourceType", sourceType)));
        request.setConflicts("proceed");

        try {
            BulkByScrollResponse response = esClient.deleteByQuery(request, RequestOptions.DEFAULT);
            log.info("shop user-resource delete success: count={}", response.getDeleted());
            return true;
        } catch (Exception e) {
            log.error("shop user-resource delete failed", e);
            return false;
        }
    }

    private void validateShop(ShopDoc data) {
        if (data == null) {
            throw new IllegalArgumentException("shop document is required");
        }
        if (data.getId() == null) {
            throw new IllegalArgumentException("shop id is required");
        }
        if (data.getName() == null) {
            throw new IllegalArgumentException("shop name is required");
        }
    }
}
