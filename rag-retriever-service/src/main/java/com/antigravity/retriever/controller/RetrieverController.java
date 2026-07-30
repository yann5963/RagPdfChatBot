package com.antigravity.retriever.controller;

import com.antigravity.common.ChatRequest;
import com.antigravity.common.ChatResponse;
import com.antigravity.retriever.service.RagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class RetrieverController {

    private final RagService ragService;

    public RetrieverController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest payload) {
        String question = payload.question();
        String model = payload.model() != null ? payload.model() : "glm-4.6:cloud";
        String answer = ragService.generateResponse(question, model);
        return new ChatResponse(answer);
    }
}
