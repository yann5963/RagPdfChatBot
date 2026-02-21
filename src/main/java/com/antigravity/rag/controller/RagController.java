package com.antigravity.rag.controller;

import com.antigravity.rag.service.RagService;
import com.antigravity.rag.service.VectorIngestionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Controller
public class RagController {

    private final VectorIngestionService ingestionService;
    private final RagService ragService;

    public RagController(VectorIngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    @GetMapping("/")
    public String home() {
        return "chat";
    }

    @PostMapping("/api/ingest")
    @ResponseBody
    public Map<String, String> ingest(@RequestParam("file") MultipartFile file) {
        try {
            ingestionService.ingest(file);
            return Map.of("status", "success", "message", "File ingested successfully");
        } catch (IOException e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String answer = ragService.generateResponse(question);
        return Map.of("answer", answer);
    }
}
