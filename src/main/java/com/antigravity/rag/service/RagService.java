package com.antigravity.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/rag-prompt-template.st")
    private org.springframework.core.io.Resource ragPromptTemplate;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public String generateResponse(String message) {
        List<Document> similarDocuments = vectorStore
                .similaritySearch(SearchRequest.builder().query(message).topK(3).build());
        String content = similarDocuments != null ? similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n")) : "";

        return chatClient.prompt()
                .user(message)
                .system(s -> s.text(ragPromptTemplate)
                        .param("documents", content)
                        .param("input", message))
                .call()
                .content();
    }
}
