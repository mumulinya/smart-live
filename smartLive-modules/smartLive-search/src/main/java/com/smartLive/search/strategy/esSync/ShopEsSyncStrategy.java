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

/**
 * 店铺数据同步策略实现
 * 负责维护 LBS 地理位置索引，并处理店铺名、坐标、评分等关键字段在用户资源表中的级联映射。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
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

    /**
     * 同步或更新店铺索引
     * 更新店铺信息（如搬迁导致的坐标变动）后，会自动触发周边搜索和用户收藏夹的坐标同步。
     */
    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        ShopDoc doc = EsTool.convertToObject((Map) data, ShopDoc.class);
        validateShop(doc);
        
        String json = objectMapper.writeValueAsString(data);
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
        
        if (response.status() == RestStatus.CREATED || response.status() == RestStatus.OK) {
            log.info("店铺索引更新成功: id={}", id);
            // 级联更新用户资源宽表中的店铺快照
            UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                    .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                    .sourceType(GlobalBizTypeEnum.SHOP.getCode())
                    .sourceId(doc.getId())
                    .data(doc)
                    .build();
            updateUserResource(userResourceMessage);
            return true;
        }
        return false;
    }

    /**
     * 批量店鋪导入
     */
    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        List<ShopDoc> docList = EsTool.convertList(dataList, ShopDoc.class);
        if (docList.isEmpty()) return true;

        BulkRequest bulkRequest = new BulkRequest();
        for (ShopDoc data : docList) {
            validateShop(data);
            String id = data.getId().toString();
            String json = objectMapper.writeValueAsString(data);
            bulkRequest.add(new IndexRequest(indexName).id(id).source(json, XContentType.JSON));
        }
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !response.hasFailures();
    }

    /**
     * 删除店铺
     * 级联删除该店铺在所有用户侧的收藏数据。
     */
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

    /**
     * 店铺关联资源级联更新
     * 同步字段：店名(映射为标题)、封面、商圈/地址(映射为副标题)、评分、销量、均价以及核心的地理位置坐标(location)。
     */
    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        if (msg.getSourceId() == null || msg.getSourceType() == null) return false;

        ShopDoc shop = objectMapper.convertValue(msg.getData(), ShopDoc.class);
        if (shop == null) return false;

        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        Map<String, Object> params = new HashMap<>();
        if (shop.getName() != null) params.put("title", shop.getName());
        
        if (shop.getImages() != null && !shop.getImages().isEmpty()) {
            params.put("imageUrl", shop.getImages().split(",")[0]);
        }
        
        if (shop.getArea() != null) {
            params.put("subTitle", shop.getArea());
        } else if (shop.getAddress() != null) {
            params.put("subTitle", shop.getAddress());
        }

        if (shop.getScore() != null) params.put("score", shop.getScore());
        if (shop.getSold() != null) params.put("sold", shop.getSold());
        if (shop.getAvgPrice() != null) params.put("avgPrice", shop.getAvgPrice());
        if (shop.getLocation() != null) params.put("location", shop.getLocation());

        try {
            params.put("payload", objectMapper.writeValueAsString(shop));
        } catch (Exception e) {}

        if (params.isEmpty()) return true;

        String loopScript = "for (entry in params.entrySet()) { ctx._source[entry.getKey()] = entry.getValue(); }";
        request.setScript(new Script(ScriptType.INLINE, "painless", loopScript, params));
        request.setConflicts("proceed");
        
        try {
            BulkByScrollResponse response = esClient.updateByQuery(request, RequestOptions.DEFAULT);
            log.info("店铺级联同步追踪完成: id={}, count={}", msg.getSourceId(), response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("店铺级联更新故障", e);
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
            log.info("店铺永久移除，同步清除用户收藏数据: count={}", response.getDeleted());
            return true;
        } catch (Exception e) {
            log.error("店铺资源清除失败", e);
            return false;
        }
    }

    private void validateShop(ShopDoc data) {
        if (data.getId() == null) throw new IllegalArgumentException("店铺ID必填");
        if (data.getName() == null) throw new IllegalArgumentException("店铺名称必填");
    }
}
