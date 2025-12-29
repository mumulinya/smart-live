package com.smartLive.ai.service.business.impl;

import com.smartLive.ai.entity.DOC.ShopDoc;
import com.smartLive.ai.service.business.IShopMilvusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ShopMilvusServiceImpl implements IShopMilvusService {

    @Autowired
    @Qualifier("shopVectorStore")
    private VectorStore shopVectorStore;

    @Override
    public boolean insertOrUpdate(String id, ShopDoc data) throws IOException {
        // 删除
        delete(id);
        Document document = createDocument(data);
        List<Document> list=new ArrayList<>();
        list.add(document);
        shopVectorStore.add(list);
        return true;
    }

    @Override
    public boolean batchInsert(List<ShopDoc> dataList) throws IOException {
        // 分批处理，每批10个
        int batchSize = 10;
        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<ShopDoc> batch = dataList.subList(i, end);

            // 转换为Document并添加元数据
            List<Document> documents = batch.stream()
                    .map(shop -> {
                        return createDocument(shop);
                    })
                    .toList();
            System.out.println("写入第 " + (i/batchSize + 1) + " 批数据，数量: " + documents.size());
            shopVectorStore.add(documents);
        }
        return true;
    }

    @Override
    public boolean delete(String id) throws IOException {
        // 1. 构建Spring AI的过滤条件（匹配元数据中的id）
        // 语法与你查询时的filterExpression一致：数值类型直接写，字符串用单引号
        String filterExpr = String.format("id == %s", id);

        // 2. 执行删除（Spring AI VectorStore的delete方法）
        // Spring AI 2.x的MilvusVectorStore.delete支持过滤表达式/String类型的filter
        shopVectorStore.delete(filterExpr);

        return true;
    }
    private Document createDocument(ShopDoc shop) {
        // 1. 文档内容：需要被搜索的文本
        String content = String.format("%s %s %s",
                shop.getName(), shop.getArea(), shop.getAddress());

        // 2. 元数据：用于过滤的固定值
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", shop.getId());
        metadata.put("name", shop.getName());
        metadata.put("area", shop.getArea());
        metadata.put("address", shop.getAddress());
        metadata.put("x", shop.getX());
        metadata.put("y", shop.getY());
        metadata.put("sold", shop.getSold());
        metadata.put("comments", shop.getComments());
        metadata.put("openHours", shop.getOpenHours());
        metadata.put("images", shop.getImages());
        metadata.put("typeId", shop.getTypeId());
        metadata.put("avgPrice", shop.getAvgPrice());
        metadata.put("score", shop.getScore());
        return new Document(content, metadata);
    }
}