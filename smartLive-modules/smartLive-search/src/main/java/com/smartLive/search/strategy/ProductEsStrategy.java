package com.smartLive.search.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ShopDoc;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ProductEsStrategy implements EsSyncStrategy {
    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ObjectMapper objectMapper;


    /**
     * 获取策略的类型
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.PRODUCT.getCode();
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
        ProductDoc doc = convertToProductDoc((Map<String, Object>) data);
        // ProductDoc doc = EsTool.convertToObject((Map) data, ProductDoc.class);
        // 1. 数据校验
        validateProduct(doc);
        // 2. 转换为JSON
        String json = objectMapper.writeValueAsString(data);
        // 3. 执行ES插入/更新（ID存在则更新，不存在则插入）
        IndexRequest request = new IndexRequest(indexName)
                .id(id)
                .source(json, XContentType.JSON);
        IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);

        if (response.status() == RestStatus.CREATED
                || response.status() == RestStatus.OK) {

            log.info("商品ES插入/更新成功：index={}, id={}, result={}",
                    indexName, id, response.getResult());
            UserResourceMessage userResourceMessage = UserResourceMessage
                    .builder()
                    .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                    .sourceType(GlobalBizTypeEnum.PRODUCT.getCode())
                    .sourceId(doc.getId())
                    .data(doc)
                    .build();
            updateUserResource(userResourceMessage);
            return true;
        }

        log.error("商品ES插入/更新失败：index={}, id={}, status={}, result={}",
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
        // List<ProductDoc> docList = EsTool.convertList(dataList, ProductDoc.class);
        List<ProductDoc> docList = new java.util.ArrayList<>();
        for (Object data : dataList) {
            docList.add(convertToProductDoc((Map<String, Object>) data));
        }
        if (docList.isEmpty()) {
            log.warn("商品批量插入数据为空：index={}", indexName);
            return true;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (ProductDoc data : docList) {
            // 1. 逐条校验
            validateProduct(data);
            // 2. 生成ID并添加到批量请求
            String id = data.getId().toString();
            String json = objectMapper.writeValueAsString(data);
            bulkRequest.add(new IndexRequest(indexName).id(id).source(json, XContentType.JSON));
        }
        // 3. 执行批量操作
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (response.hasFailures()) {
            log.error("商品批量插入失败：index={}, failures={}", indexName, response.buildFailureMessage());
            return false;
        }else {
            log.info("商品批量插入成功：index={}, 数量={}", indexName, dataList.size());
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
            log.error("商品ES删除失败：index={}, id={}, status={}", indexName, id, delete.status());
            return false;
        }
        log.info("商品ES删除成功：index={}, id={}", indexName, id);
        //删除用户资源
        deleteUserResourceBySource(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME, Long.valueOf(id), GlobalBizTypeEnum.PRODUCT.getCode());
        return true;
    }
    /**
     * @param
     * @return
     */
    @Override
    public boolean insertUserResource(UserResourceMessage userResourceMessage) throws IOException {
        ProductDoc doc = convertToProductDoc((Map<String, Object>) userResourceMessage.getData());
        // ProductDoc doc = EsTool.convertToObject((Map) userResourceMessage.getData(), ProductDoc.class);
        // 1. 数据校验
        validateProduct(doc);
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
     * 商品全量字段更新
     * 同步：标题、副标题、店铺名、规则、价格、时间、类型、状态 以及 Payload
     */
    @Override
    public boolean updateUserResource(UserResourceMessage msg) throws IOException {
        // 1. 校验
        if (msg.getSourceId() == null || msg.getSourceType() == null) {
            log.error("商品更新失败：sourceId 或 sourceType 为空");
            return false;
        }

        // 2. 数据转换
        ProductDoc product = objectMapper.convertValue(msg.getData(), ProductDoc.class);
        if (product == null) {
            return false;
        }

        // 3. 准备 Request
        UpdateByQueryRequest request = new UpdateByQueryRequest(msg.getIndexName());
        request.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("sourceId", msg.getSourceId()))
                .must(QueryBuilders.termQuery("sourceType", msg.getSourceType())));

        // 4. ★★★ 准备脚本参数 (全字段映射) ★★★
        Map<String, Object> params = new HashMap<>();

        // --- A. 通用搜索/展示字段 (映射到 ES 标准字段) ---
        if (product.getName() != null) params.put("name", product.getName());
        if (product.getSubTitle() != null) params.put("subTitle", product.getSubTitle());
        // 商品的"店铺名称" -> ES的"name"
        if (product.getShopName() != null) params.put("Shopname", product.getShopName());

        // --- B. 商品业务字段 (映射到 ES 同名字段) ---
        // 价格
        if (product.getPrice() != null) params.put("price", product.getPrice());
        if (product.getOriginalPrice() != null) params.put("originalPrice", product.getOriginalPrice()); 

        // 规则与类型
        if (product.getRulesJson() != null) params.put("rulesJson", product.getRulesJson());
        if (product.getActivityType() != null) params.put("activityType", product.getActivityType()); // 0普通 1秒杀
        if (product.getStatus() != null) params.put("status", product.getStatus());

        // 时间相关 (用于排序或判断过期)
        if (product.getBeginTime() != null) params.put("beginTime", product.getBeginTime());
        if (product.getEndTime() != null) params.put("endTime", product.getEndTime());
        if (product.getUseStartTime() != null) params.put("useStartTime", product.getUseStartTime());
        if (product.getUseEndTime() != null) params.put("useEndTime", product.getUseEndTime());
        if (product.getValidDays() != null) params.put("validDays", product.getValidDays());
        if (product.getValidityType() != null) params.put("validityType", product.getValidityType());

        // --- C. ★ 核心：更新 Payload (完整数据快照) ---
        // 将整个 product 对象转为 JSON 字符串存入 payload
        // 这样前端获取详情时，可以直接解析 payload 拿到上面所有字段，甚至包括库存等生僻字段
        try {
            String payloadJson = objectMapper.writeValueAsString(product);
            params.put("payload", payloadJson);
        } catch (Exception e) {
            log.warn("Payload序列化失败", e);
        }

        // 5. 动态构建 Script
        StringBuilder scriptCode = new StringBuilder();

        // 循环遍历 params，自动生成赋值语句
        // 效果等同于：ctx._source.title = params.title; ctx._source.rules = params.rules; ...
        // 注意：这里用 entrySet 遍历，代码非常简洁，不需要写几十个 if
        String loopScript =
                "for (entry in params.entrySet()) { " +
                        "  if (entry.getKey().equals('name')) { " +
                        "      ctx._source.name = entry.getValue(); " + // 特殊处理 name 字段
                        "  } else { " +
                        "      ctx._source[entry.getKey()] = entry.getValue(); " + // 其他字段同名赋值
                        "  } " +
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
            log.info("商品全字段同步成功，sourceId={}, 影响={}", msg.getSourceId(), response.getUpdated());
            return true;
        } catch (Exception e) {
            log.error("商品同步失败", e);
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
     * 商品数据校验（特有的校验逻辑）
     */
    private void validateProduct(ProductDoc data) {
        if (data.getId() == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (data.getName() == null || data.getName().isEmpty()) {
            throw new IllegalArgumentException("商品名称不能为空");
        }
    }

    private ProductDoc convertToProductDoc(Map<String, Object> data) {
        ProductDoc doc = EsTool.convertToObject(data, ProductDoc.class);
        return doc;
    }
}
