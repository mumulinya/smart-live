//package com.smartLive.ai;
//
//import com.smartLive.ai.entity.query.ShopQuery;
//import com.smartLive.ai.entity.vo.CommentVO;
//import com.smartLive.ai.entity.vo.Result;
//import com.smartLive.ai.entity.vo.ShopVO;
//import com.smartLive.ai.entity.vo.VoucherVO;
//import com.smartLive.ai.feign.CommentClient;
//import com.smartLive.ai.feign.OrderClient;
//import com.smartLive.ai.feign.ShopClient;
//import com.smartLive.ai.feign.VoucherClient;
//import com.smartLive.ai.service.strategy.handlers.impl.CommentHandler;
//import com.smartLive.ai.utils.VectorDistanceUtils;
//import org.junit.jupiter.api.Test;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.openai.OpenAiEmbeddingModel;
//import org.springframework.ai.reader.ExtractedTextFormatter;
//import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
//import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.core.io.Resource;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@SpringBootTest
//class SmartLiveAiApplicationTests {
//
//    @Autowired
//    private OpenAiEmbeddingModel embeddingModel;
//
//    @Autowired
//    @Qualifier("shopVectorStore")
//    private VectorStore shopVectorStore;
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    @Autowired
//    private VoucherClient voucherClient;
//
//    @Autowired
//    @Qualifier("voucherVectorStore")
//    private VectorStore voucherVectorStore;
//
//    @Autowired
//    @Qualifier("commentVectorStore")
//    private VectorStore commentVectorStore;
//
//    @Autowired
//    private ShopClient shopClient;
//    @Autowired
//    private CommentClient commentClient;
//
//    @Autowired
//    private CommentHandler commentHandler;
//
//    @Autowired
//    private OrderClient orderClient;
//    @Test
//    public void testVectorStore(){
//        Resource resource = new FileSystemResource("D:/智评生活/heima-ai/中二知识笔记.pdf");
//        // 1.创建PDF的读取器
//        PagePdfDocumentReader reader = new PagePdfDocumentReader(
//                resource, // 文件源
//                PdfDocumentReaderConfig.builder()
//                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
//                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
//                        .build()
//        );
//        // 2.读取PDF文档，拆分为Document
//        List<Document> documents = reader.read();
//        // 3.写入向量库
//        vectorStore.add(documents);
//        // 4.搜索
//        SearchRequest request = SearchRequest.builder()
//                .query("论语中教育的目的是什么")
//                .topK(1)
//                .similarityThreshold(0.6)
//                .filterExpression("file_name == '中二知识笔记.pdf'")
//                .build();
//        List<Document> docs = vectorStore.similaritySearch(request);
//        if (docs == null) {
//            System.out.println("没有搜索到任何内容");
//            return;
//        }
//        for (Document doc : docs) {
//            System.out.println(doc.getId());
//            System.out.println(doc.getScore());
//            System.out.println(doc.getText());
//        }
//    }
//
//    @Test
//    void contextLoads() {
//        // 1.测试数据
//        // 1.1.用来查询的文本，国际冲突
//        String query = "global conflicts";
//
//        // 1.2.用来做比较的文本
//        String[] texts = new String[]{
//                "哈马斯称加沙下阶段停火谈判仍在进行 以方尚未做出承诺",
//                "土耳其、芬兰、瑞典与北约代表将继续就瑞典“入约”问题进行谈判",
//                "日本航空基地水井中检测出有机氟化物超标",
//                "国家游泳中心（水立方）：恢复游泳、嬉水乐园等水上项目运营",
//                "我国首次在空间站开展舱外辐射生物学暴露实验",
//        };
//        // 2.向量化
//        // 2.1.先将查询文本向量化
//        float[] queryVector = embeddingModel.embed(query);
//
//        // 2.2.再将比较文本向量化，放到一个数组
//        List<float[]> textVectors = embeddingModel.embed(Arrays.asList(texts));
//
//        // 3.比较欧氏距离
//        // 3.1.把查询文本自己与自己比较，肯定是相似度最高的
//        System.out.println(VectorDistanceUtils.euclideanDistance(queryVector, queryVector));
//        // 3.2.把查询文本与其它文本比较
//        for (float[] textVector : textVectors) {
//            System.out.println(VectorDistanceUtils.euclideanDistance(queryVector, textVector));
//        }
//        System.out.println("------------------");
//
//        // 4.比较余弦距离
//        // 4.1.把查询文本自己与自己比较，肯定是相似度最高的
//        System.out.println(VectorDistanceUtils.cosineDistance(queryVector, queryVector));
//        // 4.2.把查询文本与其它文本比较
//        for (float[] textVector : textVectors) {
//            System.out.println(VectorDistanceUtils.cosineDistance(queryVector, textVector));
//        }
//    }
//    @Test
//    public void testEs() {
//        try {
//            // 测试向量库连接和基本操作
//            System.out.println("VectorStore 类型: " + vectorStore.getClass().getName());
//            System.out.println("VectorStore 初始化成功！");
//        } catch (Exception e) {
//            System.err.println("VectorStore 初始化失败: " + e.getMessage());
//        }
//    }
//
//    @Test
//    public void insertShop() {
//        // 确保集合存在
//        try {
//
//
//            //添加店铺数据
//            List<ShopVO> shopList = shopClient.searchShopsByCategory(new ShopQuery());
//            System.out.printf("搜索到的数据数量: " + shopList.size());
//
//          // 分批处理，每批10个
//            int batchSize = 10;
//            for (int i = 0; i < shopList.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, shopList.size());
//                List<ShopVO> batch = shopList.subList(i, end);
//
//                // 转换为Document并添加元数据
//                List<Document> documents = batch.stream()
//                        .map(shop -> {
//                            // 1. 文档内容：需要被搜索的文本
//                            String content = String.format("%s %s %s",
//                                    shop.getName(), shop.getArea(), shop.getAddress());
//
//                            // 2. 元数据：用于过滤的固定值
//                            Map<String, Object> metadata = new HashMap<>();
//                            metadata.put("id", shop.getId());
//                            metadata.put("name", shop.getName());
//                            metadata.put("area", shop.getArea());
//                            metadata.put("address", shop.getAddress());
//                            metadata.put("x", shop.getX());
//                            metadata.put("y", shop.getY());
//                            metadata.put("sold", shop.getSold());
//                            metadata.put("comments", shop.getComments());
//                            metadata.put("openHours", shop.getOpenHours());
//                            metadata.put("images", shop.getImages());
//                            metadata.put("typeId", shop.getTypeId());
//                            metadata.put("avgPrice", shop.getAvgPrice());
//                            metadata.put("score", shop.getScore());
//
//                            return new Document(content, metadata);
//                        })
//                        .toList();
//
//                System.out.println("写入第 " + (i/batchSize + 1) + " 批数据，数量: " + documents.size());
//                shopVectorStore.add(documents);
//            }
//
//
//            List<Document> results = this.shopVectorStore.similaritySearch(
//                    SearchRequest.builder()
//                            .query("大关附近有美食吗")
//                            .topK(10)
//                            .filterExpression("avgPrice >= 50 && avgPrice <= 100")
//                            .build());
//            results.forEach(r -> System.out.println(r.getText()+r.getScore()+r.getMedia()));
//        } catch (Exception e) {
//            System.err.println("Milvus 操作失败: " + e.getMessage());
//            // 可能需要检查 Milvus 服务状态或重新配置
//        }
//    }
//    @Test
//    public void insertVoucher() {
//        try {
//
////            //添加优惠券数据
//            List<VoucherVO> voucherVOList = voucherClient.listVoucher();
//            System.out.printf("搜索到的数据数量: " + voucherVOList.size());
//            System.out.println("数据为"+voucherVOList);
//            // 分批处理，每批10个
//            int batchSize = 10;
//            for (int i = 0; i < voucherVOList.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, voucherVOList.size());
//                List<VoucherVO> batch = voucherVOList.subList(i, end);
//
//                // 转换为Document并添加元数据
//                List<Document> documents = batch.stream()
//                        .map(voucher -> {
//                            // 1. 文档内容：需要被搜索的文本
//                            String content = String.format("%s %s %s",
//                                    voucher.getShopName(), voucher.getTitle(), voucher.getRules());
//
//                            // 构建元数据
//                            Map<String, Object> metadata = new HashMap<>();
//                            putIfNotNull(metadata, "id", voucher.getId());
//                            putIfNotNull(metadata, "shopId", voucher.getShopId());
//                            putIfNotNull(metadata, "typeId", voucher.getTypeId());
//                            putIfNotNull(metadata, "shopName", voucher.getShopName());
//                            putIfNotNull(metadata, "title", voucher.getTitle());
//                            putIfNotNull(metadata, "subTitle", voucher.getSubTitle());
//                            putIfNotNull(metadata, "rules", voucher.getRules());
//                            putIfNotNull(metadata, "payValue", voucher.getPayValue());
//                            putIfNotNull(metadata, "actualValue", voucher.getActualValue());
//                            putIfNotNull(metadata, "type", voucher.getType());
//                            putIfNotNull(metadata, "status", voucher.getStatus());
//                            putIfNotNull(metadata, "stock", voucher.getStock());
//                            putIfNotNull(metadata, "beginTime", voucher.getBeginTime());
//                            putIfNotNull(metadata, "endTime", voucher.getEndTime());
//                            return new Document(content, metadata);
//                        })
//                        .toList();
//                voucherVectorStore.add(documents);
//            }
////        查询店铺数据
//                List<Document> results = this.voucherVectorStore.similaritySearch(
//                        SearchRequest.builder()
//                                .query("50元代金券")
////                                .filterExpression("shopName =='168茶餐厅'")
//                                .filterExpression("status=="+1)
//                                .similarityThreshold(0.7)
//                                .topK(3).build());
//                results.forEach(r -> System.out.println(r.getText() + r.getScore() + r.getMedia()));
//            } catch(Exception e){
//                System.err.println("Milvus 操作失败: " + e.getMessage());
//                // 可能需要检查 Milvus 服务状态或重新配置
//            }
//        }
//
//        @Test
//        public void insertComment() {
//            // 确保集合存在
//            try {
//                // 添加评论数据
//                List<CommentVO> commentList = commentClient.searchCommentList(); // 获取所有评论
//                System.out.printf("搜索到的评论数据数量: " + commentList.size());
//                System.out.println("数据为"+commentList);
//
//                // 分批处理，每批10个
//                int batchSize = 10;
//                for (int i = 0; i < commentList.size(); i += batchSize) {
//                    int end = Math.min(i + batchSize, commentList.size());
//                    List<CommentVO> batch = commentList.subList(i, end);
//
//                    // 转换为Document并添加元数据
//                    List<Document> documents = batch.stream()
//                            .map(comment -> {
//                                // 1. 文档内容：需要被搜索的文本
//                                String content = String.format("%s %s %s",
//                                        comment.getContent(),
//                                        comment.getNickName() != null ? comment.getNickName() : "",
//                                        comment.getSourceName() != null ? comment.getSourceName(): "");
//
//                                // 2. 元数据：用于过滤的固定值
//                                Map<String, Object> metadata = new HashMap<>();
//                                putIfNotNull(metadata, "id", comment.getId());
//                                putIfNotNull(metadata, "userId", comment.getUserId());
//                                putIfNotNull(metadata, "sourceType", comment.getSourceType());
//                                putIfNotNull(metadata, "sourceId", comment.getSourceId());
//                                putIfNotNull(metadata, "sourceName", comment.getSourceName());
//                                putIfNotNull(metadata, "parentId", comment.getParentId());
////                                putIfNotNull(metadata, "answerId", comment.getAnswerId());
//                                putIfNotNull(metadata, "images", comment.getImages());
//                                putIfNotNull(metadata, "content", comment.getContent());
//                                putIfNotNull(metadata, "liked", comment.getLiked());
//                                putIfNotNull(metadata, "status", comment.getStatus());
//                                putIfNotNull(metadata, "rating", comment.getRating());
//                                putIfNotNull(metadata, "createTime", comment.getCreateTime());
//
//                                return new Document(content, metadata);
//                            })
//                            .toList();
//
//                    System.out.println("写入第 " + (i/batchSize + 1) + " 批评论数据，数量: " + documents.size());
//                    commentVectorStore.add(documents);
//                }
//
//                // 测试搜索评论
//                List<Document> results = this.commentVectorStore.similaritySearch(
//                        SearchRequest.builder()
//                                .query("服务很好")  // 搜索包含"服务很好"的评论
//                                .topK(10)
//                                .filterExpression("sourceType == 1 && status == '0'")  // 只查询店铺的正常评论
//                                .build());
//
//                System.out.println("搜索到的评论数量: " + results.size());
//                results.forEach(r -> System.out.println(r.getText() + " | 评分: " + r.getMetadata().get("rating") + " | 点赞: " + r.getMetadata().get("liked")));
//
//            } catch (Exception e) {
//                System.err.println("评论向量库操作失败: " + e.getMessage());
//                // 可能需要检查向量库服务状态或重新配置
//            }
//        }
//
//        @Test
//        public void aiCreateComment(){
//        CommentVO commentVO = new CommentVO();
//        commentVO.setSourceType(2);
//        commentVO.setSourceId(1L);
//        commentVO.setSourceName("海底捞火锅");
////        commentHandler.aiCreateComment(commentVO);
//        }
//    // 工具方法防止输入空值
//    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
//        if (value != null) {
//            map.put(key, value);
//        }
//    }
//
//    @Test
//    public void testOrderVoucher() {
////        orderClient.seckillVoucher(1L);
//        Result result = orderClient.buyVoucher(1L, 1010L);
//        System.out.println("订单创建结果为"+result.getSuccess());
//        System.out.println("订单创建结果为"+result.getErrorMsg());
//        System.out.println("订单id为"+result.getData());
//    }
//}
