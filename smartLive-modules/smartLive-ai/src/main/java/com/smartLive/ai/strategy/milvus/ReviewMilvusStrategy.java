package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.interaction.api.DTO.ReviewDTO;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReviewMilvusStrategy implements MilvusSyncStrategy<ReviewDTO> {

    @Autowired
    @Qualifier("reviewVectorStore")
    private VectorStore reviewVectorStore;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.REVIEW.getCode();
    }

    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        ReviewDTO doc = EsTool.convertToObject((Map) rawData, ReviewDTO.class);

        // 删除旧数据
        delete(id);

        Document document = null;
        if (doc != null) {
            document = createDocument(doc);
        }
        if (document != null) {
            List<Document> list = new ArrayList<>();
            list.add(document);
            reviewVectorStore.add(list);
        }
        return true;
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<ReviewDTO> list = EsTool.convertList(rawDataList, ReviewDTO.class);

        int batchSize = 10;
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<ReviewDTO> batch = list.subList(i, end);

            // 提取这一批的所有 id，提前删除
            List<String> idList = batch.stream()
                    .map(review -> String.valueOf(review.getId()))
                    .toList();
            if (!idList.isEmpty()) {
                String inExpr = String.join(", ", idList);
                String filterExpr = String.format("id in [%s]", inExpr);
                reviewVectorStore.delete(filterExpr);
            }

            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .toList();
            System.out.println("写入第 " + (i / batchSize + 1) + " 批评论数据，数量: " + documents.size());
            reviewVectorStore.add(documents);
        }
        return true;
    }

    @Override
    public boolean delete(String id) throws IOException {
        String filterExpr = String.format("id == %s", id);
        reviewVectorStore.delete(filterExpr);
        return true;
    }

    @Override
    public Document createDocument(ReviewDTO review) {
        String content = String.format("%s", review.getContent());

        Map<String, Object> metadata = new HashMap<>();

        // === 基础字段（必需）===
        putIfNotNull(metadata, "id", review.getId());
        putIfNotNull(metadata, "userId", review.getUserId());
        putIfNotNull(metadata, "shopId", review.getShopId());
        putIfNotNull(metadata, "sourceId", review.getSourceId());
        putIfNotNull(metadata, "sourceType", review.getSourceType());

        // === 过滤字段 ===
        putIfNotNull(metadata, "status", review.getStatus());
        putIfNotNull(metadata, "isAIGenerated", review.getIsAIGenerated());

        // === 评分字段（用于排序和推荐） ===
        putIfNotNull(metadata, "score", review.getScore());
        putIfNotNull(metadata, "serviceScore", review.getServiceScore());
        putIfNotNull(metadata, "tasteScore", review.getTasteScore());
        putIfNotNull(metadata, "envScore", review.getEnvScore());

        // === 热度指标（用于排序） ===
        metadata.put("liked", review.getLiked() != null ? review.getLiked() : 0);
        metadata.put("replyCount", review.getReplyCount() != null ? review.getReplyCount() : 0);
        metadata.put("stared", review.getStared() != null ? review.getStared() : 0);

        // === 展示字段 ===
        putIfNotNull(metadata, "nickName", review.getNickName());
        putIfNotNull(metadata, "userIcon", review.getUserIcon());
        putIfNotNull(metadata, "images", review.getImages());

        // === 时间戳（用于排序） ===
        metadata.put("createTime", review.getCreateTime() != null ?
                review.getCreateTime().getTime() : System.currentTimeMillis());

        // === 其他信息 ===
        metadata.put("isAnonymous", review.getIsAnonymous() != null ? review.getIsAnonymous() : false);
        putIfNotNull(metadata, "sourceName", review.getSourceName());

        return new Document(content, metadata);
    }
    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
