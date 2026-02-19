package com.smartLive.ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.milvus")
@Data
@Slf4j
public class milvusConfig {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private String databaseName;
    private IndexType indexType;
    private MetricType metricType;
    private Boolean initializeSchema;

    /**
     * Set false to skip Milvus and always use in-memory vector store.
     */
    private Boolean enabled = true;

    private transient volatile MilvusServiceClient cachedMilvusClient;
    private transient volatile boolean milvusClientInitAttempted;

    @Bean
    public VectorStore commentVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return buildVectorStore("comment", embeddingModel);
    }

    @Bean
    public VectorStore shopVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return buildVectorStore("shop", embeddingModel);
    }

    @Bean
    public VectorStore productVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        // Changed collection name to 'product'
        return buildVectorStore("product", embeddingModel);
    }

    private VectorStore buildVectorStore(String collectionName, EmbeddingModel embeddingModel) {
        MilvusServiceClient milvusClient = getMilvusClientOrNull();
        if (milvusClient == null) {
            log.warn("Milvus unavailable, using SimpleVectorStore fallback. collection={}", collectionName);
            return SimpleVectorStore.builder(embeddingModel).build();
        }
        return getMilvusVectorStoreBuilder(milvusClient, embeddingModel)
                .collectionName(collectionName)
                .build();
    }

    private MilvusServiceClient getMilvusClientOrNull() {
        if (Boolean.FALSE.equals(enabled)) {
            log.info("Milvus is disabled by config: spring.ai.vectorstore.milvus.enabled=false");
            return null;
        }

        if (milvusClientInitAttempted) {
            return cachedMilvusClient;
        }

        synchronized (this) {
            if (milvusClientInitAttempted) {
                return cachedMilvusClient;
            }
            milvusClientInitAttempted = true;

            if (!StringUtils.hasText(host) || port == null) {
                log.warn("Milvus host/port not configured, fallback to SimpleVectorStore. host={}, port={}", host, port);
                return null;
            }

            try {
                ConnectParam.Builder builder = ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port);

                if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                    builder.withAuthorization(username, password);
                }

                cachedMilvusClient = new MilvusServiceClient(builder.build());
                log.info("Milvus client initialized. host={}, port={}", host, port);
                return cachedMilvusClient;
            } catch (Exception ex) {
                log.error(
                        "Milvus init failed, fallback to SimpleVectorStore. host={}, port={}, reason={}",
                        host,
                        port,
                        ex.getMessage(),
                        ex
                );
                cachedMilvusClient = null;
                return null;
            }
        }
    }

    public MilvusVectorStore.Builder getMilvusVectorStoreBuilder(
            MilvusServiceClient milvusClient,
            EmbeddingModel embeddingModel
    ) {
        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .indexType(indexType)
                .metricType(metricType)
                .initializeSchema(initializeSchema);
    }
}
