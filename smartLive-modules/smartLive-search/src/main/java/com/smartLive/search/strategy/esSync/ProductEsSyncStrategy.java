package com.smartLive.search.strategy.esSync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.search.domain.ProductDoc;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品/代金券数据同步策略实现
 * 负责维护商品主索引，并在用户资源库（收藏记录）中同步商品状态、价格及规则快照。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Component
@Slf4j
public class ProductEsSyncStrategy implements EsSyncStrategy {
    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.PRODUCT.getCode();
    }

    /**
     * 同步单条商品
     */
    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        ProductDoc doc = EsTool.convertToObject((Map) data, ProductDoc.class);
        validateProduct(doc);
        
        String json = objectMapper.writeValueAsString(EsTool.convertToJsonMap(doc));
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);

        if (response.status() == RestStatus.CREATED || response.status() == RestStatus.OK) {
            log.info("商品索引已更新: id={}", id);
            // 触发用户收藏夹内该商品快照的全量同步
            UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                    .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                    .sourceType(GlobalBizTypeEnum.PRODUCT.getCode())
                    .sourceId(doc.getId())
                    .data(doc)
                    .build();
            updateUserResource(userResourceMessage);
            return true;
        }
        return false;
    }

    /**
     * 批量同步商品数据
     */
    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        List<ProductDoc> docList = new ArrayList<>();
        for (Object data : dataList) {
            docList.add(EsTool.convertToObject((Map) data, ProductDoc.class));
        }
        if (docList.isEmpty()) return true;

        BulkRequest bulkRequest = new BulkRequest();
        for (ProductDoc data : docList) {
            validateProduct(data);
            String json = objectMapper.writeValueAsString(EsTool.convertToJsonMap(data));
            bulkRequest.add(new IndexRequest(indexName).id(data.getId().toString()).source(json, XContentType.JSON));
        }
        
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !response.hasFailures();
    }

    /**
     * 删除商品
     */
    @Override
    public boolean delete(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        DeleteResponse delete = esClient.delete(request, RequestOptions.DEFAULT);
        if (delete.status() == RestStatus.OK) {
            log.info("商品物理删除成功: id={}", id);
            // 级联移除用户关联的该商品资源记录
            deleteUserResourceBySource(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME, Long.valueOf(id), GlobalBizTypeEnum.PRODUCT.getCode());
            return true;
        }
        return false;
    }

    /**
     * 商品资源级联全字段同步
     * 核心逻辑：利用 Painless 脚本，只要修改了商品名称、价格、有效期、规则或状态，
     * 都会自动反映到所有领取过/收藏过该商品的用户资源记录中。
     */
    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        if (msg.getSourceId() == null || msg.getSourceType() == null) return false;

        ProductDoc product = objectMapper.convertValue(msg.getData(), ProductDoc.class);
        if (product == null) return false;

        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        Map<String, Object> params = new HashMap<>();
        if (product.getName() != null) params.put("name", product.getName());
        if (product.getSubTitle() != null) params.put("subTitle", product.getSubTitle());
        if (product.getShopName() != null) params.put("ShopName", product.getShopName());
        if (product.getPrice() != null) params.put("price", product.getPrice());
        if (product.getRulesJson() != null) params.put("rulesJson", product.getRulesJson());
        if (product.getStatus() != null) params.put("status", product.getStatus());
        if (product.getCoverImg() != null) params.put("coverImg", product.getCoverImg());

        // 存储完整数据快照作为 Payload
        try {
            params.put("payload", objectMapper.writeValueAsString(product));
        } catch (Exception e) {
            log.warn("Payload 序列化瓶颈", e);
        }

        // 利用 Painless 循环脚本实现紧凑更新
        String loopScript = "for (entry in params.entrySet()) { " +
                            "  if (entry.getKey().equals('name')) { ctx._source.name = entry.getValue(); } " +
                            "  else { ctx._source[entry.getKey()] = entry.getValue(); } " +
                            "}";

        request.setScript(new Script(ScriptType.INLINE, "painless", loopScript, params));
        request.setConflicts("proceed");
        try {
            BulkByScrollResponse response = esClient.updateByQuery(request, RequestOptions.DEFAULT);
            log.info("商品维度级联更新完成: sourceId={}, updated={}", msg.getSourceId(), response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("商品级联同步失败", e);
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
            log.info("商品物理删除触发资源同步清除: count={}", response.getDeleted());
            return true;
        } catch (Exception e) {
            log.error("商品资源清除失败", e);
            return false;
        }
    }

    private void validateProduct(ProductDoc data) {
        if (data.getId() == null) throw new IllegalArgumentException("商品ID必填");
        if (data.getName() == null) throw new IllegalArgumentException("商品标题必填");
    }
}
