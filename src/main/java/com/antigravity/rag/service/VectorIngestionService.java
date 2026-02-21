package com.antigravity.rag.service;

import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class VectorIngestionService {

    private final VectorStore vectorStore;

    /**
     * Constructeur pour injecter le VectorStore de Spring AI.
     *
     * @param vectorStore L'instance de VectorStore configurée.
     */
    public VectorIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Lit un fichier PDF, le découpe en segments (tokens) et les insère dans la
     * base de données vectorielle.
     * 
     * @param file Le fichier PDF soumis par l'utilisateur.
     * @throws IOException En cas d'erreur de lecture du fichier.
     */
    public void ingest(MultipartFile file) throws IOException {
        Resource resource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        TokenTextSplitter splitter = new TokenTextSplitter(500, 50, 5, 1000, true);

        vectorStore.accept(splitter.apply(reader.get()));
    }
}
