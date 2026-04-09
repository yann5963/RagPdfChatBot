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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.annotation.PostConstruct;

@Service
public class VectorIngestionService {
    private static final int TOKEN_CHUNK_SIZE = 500;
    private static final int CHAR_CHUNK_SIZE = 2000;
    private static final int CHAR_CHUNK_OVERLAP = 200;
    private static final int MIN_CHUNK_SIZE_CHARS = 50;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 1000;
    private static final boolean KEEP_SEPARATOR = true;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    // Set for keeping track of the files currently stored in the DB
    private final Set<String> ingestedFiles = new LinkedHashSet<>();

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
     * S'exécute au démarrage de l'application.
     * Crée la table si nécessaire et charge l'historique des fichiers en mémoire.
     */
    @PostConstruct
    public void init() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS ingested_files (" +
                        "filename VARCHAR(255) PRIMARY KEY" +
                        ")");

        List<String> filesInDb = jdbcTemplate.queryForList("SELECT filename FROM ingested_files", String.class);
        ingestedFiles.addAll(filesInDb);
    }

    /**
     * Lit un fichier PDF, le découpe en segments (tokens ou récursif) et les insère
     * dans la
     * base de données vectorielle.
     * 
     * @param file         Le fichier PDF soumis par l'utilisateur.
     * @param splitterType Le type de découpage ("simple" ou "recursive").
     * @throws IOException En cas d'erreur de lecture du fichier.
     */
    public void ingest(MultipartFile file, String splitterType) throws IOException {
        Resource resource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        TextSplitter splitter;
        if ("recursive".equalsIgnoreCase(splitterType)) {
            splitter = new RecursiveCharacterTextSplitter(CHAR_CHUNK_SIZE, CHAR_CHUNK_OVERLAP);
        } else {
            splitter = new TokenTextSplitter(
                    TOKEN_CHUNK_SIZE,
                    MIN_CHUNK_SIZE_CHARS,
                    MIN_CHUNK_LENGTH_TO_EMBED,
                    MAX_NUM_CHUNKS,
                    KEEP_SEPARATOR);
        }

        vectorStore.accept(splitter.apply(reader.get()));

        if (file.getOriginalFilename() != null) {
            String filename = file.getOriginalFilename();
            ingestedFiles.add(filename);

            // Persister en base de données de manière asynchrone ou synchrone
            jdbcTemplate.update(
                    "INSERT INTO ingested_files (filename) VALUES (?) ON CONFLICT (filename) DO NOTHING",
                    filename);
        }
    }

    /**
     * Vide la table vector_store de la base de données.
     */
    public void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE vector_store");
        jdbcTemplate.execute("TRUNCATE TABLE ingested_files");
        ingestedFiles.clear();
    }

    /**
     * Retourne la liste des fichiers actuellement ingérés.
     * 
     * @return L'ensemble des noms de fichiers.
     */
    public Set<String> getIngestedFiles() {
        return new LinkedHashSet<>(ingestedFiles);
    }
}
