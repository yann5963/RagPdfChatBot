package com.antigravity.ingestion.controller;

import com.antigravity.ingestion.service.VectorIngestionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final VectorIngestionService ingestionService;

    public IngestionController(VectorIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public Map<String, Object> ingest(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "splitterType", defaultValue = "simple") String splitterType) {
        try {
            ingestionService.ingest(file, splitterType);
            return Map.of("status", "success", "message", "Fichier traité avec succès", "files",
                    ingestionService.getIngestedFiles());
        } catch (IOException e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @GetMapping("/files")
    public java.util.Set<String> getIngestedFiles() {
        return ingestionService.getIngestedFiles();
    }

    @PostMapping("/clear")
    public Map<String, Object> clearDatabase() {
        try {
            ingestionService.clearDatabase();
            return Map.of("status", "success", "message", "Base de données vectorielle effacée avec succès.", "files",
                    ingestionService.getIngestedFiles());
        } catch (Exception e) {
            return Map.of("status", "error", "message", "Erreur lors de l'effacement : " + e.getMessage());
        }
    }
}
