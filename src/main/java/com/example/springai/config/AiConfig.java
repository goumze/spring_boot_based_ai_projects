package com.example.springai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI infrastructure configuration.
 *
 * <p>Provides Spring beans for the vector store used by the RAG pipeline. A {@link SimpleVectorStore}
 * (backed by an in-memory cosine-similarity index) is registered so that documents can be embedded
 * and queried without an external vector database.
 */
@Configuration
public class AiConfig {

    /**
     * Creates an in-memory {@link VectorStore} backed by the auto-configured
     * {@link EmbeddingModel}.
     *
     * <p>In production, replace this with a persistent store such as PgVector, Pinecone, or
     * Weaviate.
     *
     * @param embeddingModel the embedding model provided by Spring AI auto-configuration
     * @return a {@link SimpleVectorStore} instance
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
