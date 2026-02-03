package com.smartLive.search.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.search.domain.BlogDoc;
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
public class ShopEsStrategy implements EsSyncStrategy {
    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取策略的类型
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.SHOP.getCode();
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
        ShopDoc doc = EsTool.convertToObject((Map) data, ShopDoc.class);
        // 1. 数据校验
        validateShop(doc);
        // 2. 转换为JSON
        String json = objectMapper.writeValueAsString(data);
        // 3. 执行ES插入/更新
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
        if (response.status() == RestStatus.CREATED
                || response.status() == RestStatus.OK) {

            log.info("店铺ES插入/更新成功：index={}, id={}, result={}",
                    indexName, id, response.getResult());
            UserResourceMessage userResourceMessage = UserResourceMessage
                    .builder()
                    .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                    .sourceType(GlobalBizTypeEnum.SHOP.getCode())
                    .sourceId(doc.getId())
                    .data(doc)
                    .build();
            updateUserResource(userResourceMessage);
            return true;
        }

        log.error("店铺ES插入/更新失败：index={}, id={}, status={}, result={}",
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
        List<ShopDoc> docList = EsTool.convertList(dataList, ShopDoc.class);
        if (docList.isEmpty()) {
            log.warn("店铺批量插入数据为空：index={}", indexName);
            return true;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (ShopDoc data : docList) {
            // 1. 逐条校验
            validateShop(data);
            // 2. 生成ID并添加到批量请求
            String id = data.getId().toString();
            String json = objectMapper.writeValueAsString(data);
            bulkRequest.add(new IndexRequest(indexName).id(id).source(json, XContentType.JSON));
        }
        // 3. 执行批量操作
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (response.hasFailures()) {
            log.error("店铺批量插入失败：index={}, failures={}", indexName, response.buildFailureMessage());
            return false;
        }else {
            log.info("店铺批量插入成功：index={}, 数量={}", indexName, dataList.size());
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
            log.error("店铺ES删除失败：index={}, id={}, status={}", indexName, id, delete.status());
            return false;
        }
        log.info("店铺ES删除成功：index={}, id={}", indexName, id);
        //删除用户资源
        deleteUserResourceBySource(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME, Long.valueOf(id), GlobalBizTypeEnum.SHOP.getCode());
        return true;
    }
    /**
     * @param
     * @return
     */
    @Override
    public boolean insertUserResource(UserResourceMessage userResourceMessage) throws IOException {
        ShopDoc doc = EsTool.convertToObject((Map) userResourceMessage.getData(), ShopDoc.class);
        // 1. 数据校验
        validateShop(doc);
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
            log.info("店铺ES插入/更新成功：index={}, id={}, result={}",
                    userResourceMessage.getIndexName(), userResourceMessage.getId(), response.getResult());
            return true;
        }
        log.error("店铺ES插入/更新失败：index={}, id={}, status={}, result={}",
                userResourceMessage.getIndexName(), userResourceMessage.getId(), response.status(), response.getResult());
        return false;
    }


    /**
     * 店铺全量字段更新
     * 同步：店铺名(标题)、封面、商圈(副标题)、评分、销量、均价、经纬度 以及 Payload
     */
    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        // 1. 校验
        if (msg.getSourceId() == null || msg.getSourceType() == null) {
            log.error("店铺更新失败：sourceId 或 sourceType 为空");
            return false;
        }

        // 2. 数据转换
        ShopDoc shop = objectMapper.convertValue(msg.getData(), ShopDoc.class);
        if (shop == null) {
            return false;
        }

        // 3. 准备 Request
        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        // 4. ★★★ 准备脚本参数 (全字段映射) ★★★
        Map<String, Object> params = new HashMap<>();

        // --- A. 基础展示字段 ---
        // 店铺名 -> 标题
        if (shop.getName() != null) params.put("title", shop.getName());

        // 封面图处理：取第一张
        if (shop.getImages() != null && !shop.getImages().isEmpty()) {
            String cover = shop.getImages().split(",")[0];
            params.put("imageUrl", cover);
        }

        // 副标题处理：优先用商圈(area)，没有则用地址(address)
        if (shop.getArea() != null) {
            params.put("subTitle", shop.getArea());
        } else if (shop.getAddress() != null) {
            params.put("subTitle", shop.getAddress());
        }

        // --- B. 店铺核心业务字段 ---
        if (shop.getScore() != null) params.put("score", shop.getScore());       // 评分
        if (shop.getAvgPrice() != null) params.put("avgPrice", shop.getAvgPrice()); // 均价
        if (shop.getSold() != null) params.put("sold", shop.getSold());          // 销量
        if (shop.getComments() != null) params.put("comments", shop.getComments()); // 评论数

        // 地理位置 (非常重要，用于按距离排序)
        if (shop.getLocation() != null) {
            params.put("location", shop.getLocation());
        }

        // --- C. ★ 核心：更新 Payload (完整数据快照) ---
        // 将整个 shop 对象转为 JSON 字符串存入 payload
        try {
            String payloadJson = objectMapper.writeValueAsString(shop);
            params.put("payload", payloadJson);
        } catch (Exception e) {
            log.warn("Payload序列化失败", e);
        }

        // 5. 动态构建 Script (通用循环脚本)
        if (params.isEmpty()) {
            return true;
        }

        // 循环赋值脚本
        String loopScript =
                "for (entry in params.entrySet()) { " +
                        "   ctx._source[entry.getKey()] = entry.getValue(); " +
                        "}";

        request.setScript(new Script(
                ScriptType.INLINE,
                "painless",
                loopScript,
                params
        ));

        // 6. 执行
        request.setConflicts("proceed");
        try {
            BulkByScrollResponse response = esClient.updateByQuery(request, RequestOptions.DEFAULT);
            log.info("店铺全字段同步成功，sourceId={}, 影响条数={}", msg.getSourceId(), response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("店铺同步失败", e);
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
     * 店铺数据校验（特有的校验逻辑）
     */
    private void validateShop(ShopDoc data) {
        if (data.getId() == null) {
            throw new IllegalArgumentException("店铺ID不能为空");
        }
        if (data.getName() == null || data.getName().isEmpty()) {
            throw new IllegalArgumentException("店铺名称不能为空");
        }
        if (data.getTypeId() == null) {
            throw new IllegalArgumentException("店铺类型ID不能为空");
        }
        if (data.getAddress() == null || data.getAddress().isEmpty()) {
            throw new IllegalArgumentException("店铺地址不能为空");
        }
    }
}
