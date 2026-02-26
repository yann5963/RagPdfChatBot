package com.antigravity.rag.service;

import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;

@Service
public class VectorIngestionService {
    private static final int CHUNK_SIZE = 500;
    private static final int MIN_CHUNK_SIZE_CHARS = 50;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 1000;
    private static final boolean KEEP_SEPARATOR = true;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructeur pour injecter le VectorStore de Spring AI et JdbcTemplate.
     *
     * @param vectorStore  L'instance de VectorStore configurée.
     * @param jdbcTemplate L'instance de JdbcTemplate pour manipuler la BDD.
     */
    public VectorIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Lit un fichier PDF, le découpe en segments (tokens ou récursif) et les insère dans la
     * base de données vectorielle.
     * 
     * @param file Le fichier PDF soumis par l'utilisateur.
     * @param splitterType Le type de découpage ("simple" ou "recursive").
     * @throws IOException En cas d'erreur de lecture du fichier.
     */
    public void ingest(MultipartFile file, String splitterType) throws IOException {
        Resource resource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        TextSplitter splitter;
        if ("recursive".equalsIgnoreCase(splitterType)) {
            splitter = new RecursiveCharacterTextSplitter(CHUNK_SIZE, MIN_CHUNK_SIZE_CHARS);
        } else {
            splitter = new TokenTextSplitter(
                    CHUNK_SIZE,
                    MIN_CHUNK_SIZE_CHARS,
                    MIN_CHUNK_LENGTH_TO_EMBED,
                    MAX_NUM_CHUNKS,
                    KEEP_SEPARATOR);
        }

        vectorStore.accept(splitter.apply(reader.get()));
    }

    /**
     * Vide la table vector_store de la base de données.
     */
    public void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE vector_store");
    }
}
