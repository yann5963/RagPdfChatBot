package com.antigravity.ui.controller;

import com.antigravity.common.ChatRequest;
import com.antigravity.common.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class GatewayController {

    private final RestTemplate restTemplate;

    @Value("${app.services.ingestion.url}")
    private String ingestionUrl;

    @Value("${app.services.retriever.url}")
    private String retrieverUrl;

    @Value("${app.available-models}")
    private List<String> availableModels;

    public GatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("availableModels", availableModels);

        try {
            List<?> files = restTemplate.getForObject(ingestionUrl + "/api/ingest/files", List.class);
            model.addAttribute("ingestedFiles", files != null ? files : List.of());
        } catch (Exception e) {
            model.addAttribute("ingestedFiles", List.of());
        }

        return "chat";
    }

    @PostMapping("/api/ingest")
    @ResponseBody
    public Map<String, Object> ingest(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "splitterType", defaultValue = "simple") String splitterType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Wrap the file byte array to be sent correctly by RestTemplate
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            body.add("file", fileResource);
            body.add("splitterType", splitterType);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ingestionUrl + "/api/ingest", requestEntity, Map.class);
            return response.getBody();

        } catch (IOException e) {
            return Map.of("status", "error", "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("status", "error", "message", "Erreur de communication avec le service d'ingestion : " + e.getMessage());
        }
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String model = payload.getOrDefault("model", "glm-4.6:cloud");

        ChatRequest request = new ChatRequest(question, model);
        try {
            ChatResponse response = restTemplate.postForObject(retrieverUrl + "/api/chat", request, ChatResponse.class);
            return Map.of("answer", response != null ? response.answer() : "Aucune réponse du service.");
        } catch (Exception e) {
            return Map.of("answer", "Erreur de communication avec le service LLM : " + e.getMessage());
        }
    }

    @PostMapping("/api/clear")
    @ResponseBody
    public Map<String, Object> clearDatabase() {
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(ingestionUrl + "/api/ingest/clear", null, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Map.of("status", "error", "message", "Erreur de communication avec le service d'ingestion : " + e.getMessage());
        }
    }
}
