package com.antigravity.rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class VectorStoreConfig {

    /**
     * Configure et instancie le magasin vectoriel (VectorStore) en utilisant
     * PostgreSQL avec pgvector.
     * Configure les dimensions du modèle, le type de distance et le type d'index.
     *
     * @param jdbcTemplate   Le template JDBC pour interagir avec la base de
     *                       données.
     * @param embeddingModel Le modèle d'intégration de texte (ex:
     *                       OllamaEmbeddingModel).
     * @return L'instance VectorStore configurée.
     */
    @Bean
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate,
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(768)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
    }
}
