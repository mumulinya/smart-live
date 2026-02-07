package com.smartLive.ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.Data;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.milvus")
@Data
public class milvusConfig {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private String databaseName;
    private IndexType indexType;
    private MetricType metricType;
    private Boolean initializeSchema;
    @Bean
    @ConditionalOnMissingBean
    public MilvusServiceClient milvusClient() {
        return new MilvusServiceClient(ConnectParam.newBuilder()
                .withAuthorization(username, password)
                .withHost(host)
                .withPort(port)
                .build());
    }
    /**
     * 评论向量库
     */
    @Bean
    public VectorStore commentVectorStore(MilvusServiceClient milvusClient, OpenAiEmbeddingModel embeddingModel) {

        return   getMilvusVectorStoreBuilder(milvusClient, embeddingModel)
                .collectionName("comment")
                .build();
    }
    /**
     * 店铺向量库
     */
    @Bean
    public VectorStore shopVectorStore(MilvusServiceClient milvusClient, OpenAiEmbeddingModel embeddingModel) {

      return   getMilvusVectorStoreBuilder(milvusClient, embeddingModel)
              .collectionName("shop")
              .build();
    }
    /**
     * 优惠券向量库
     */
    @Bean
    public VectorStore voucherVectorStore(MilvusServiceClient milvusClient, OpenAiEmbeddingModel embeddingModel) {

        return   getMilvusVectorStoreBuilder(milvusClient, embeddingModel)
                .collectionName("voucher")
                .build();
    }
    public MilvusVectorStore.Builder getMilvusVectorStoreBuilder(MilvusServiceClient milvusClient, OpenAiEmbeddingModel embeddingModel) {

        return MilvusVectorStore.builder(milvusClient, embeddingModel)
//                .databaseName("default")
                .indexType(indexType)
                .metricType(metricType)
                .initializeSchema(initializeSchema);
    }
}
