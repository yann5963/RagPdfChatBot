package com.antigravity.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.api.OllamaOptions;
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

        /**
         * Constructeur pour initialiser le client de chat et le magasin vectoriel.
         * 
         * @param chatClientBuilder Le builder pour configurer le client Ollama.
         * @param vectorStore       Le magasin vectoriel pour la recherche de
         *                          similarité.
         */
        public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
                this.chatClient = chatClientBuilder.build();
                this.vectorStore = vectorStore;
        }

        /**
         * Génère une réponse via l'assistant LLM en utilisant le contexte récupéré
         * (RAG).
         * 
         * @param message La question posée par l'utilisateur.
         * @param model   Le modèle LLM (Ollama) à utiliser pour générer la réponse.
         * @return La réponse générée par l'assistant, basée sur les documents et la
         *         requête.
         */
        public String generateResponse(String message, String model) {
                List<Document> similarDocuments = vectorStore
                                .similaritySearch(SearchRequest.builder().query(message).topK(3).build());
                String content = similarDocuments != null ? similarDocuments.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n")) : "";

                return chatClient.prompt()
                                .user(message)
                                .options(OllamaOptions.builder().model(model).build())
                                .system(s -> s.text(ragPromptTemplate)
                                                .param("documents", content)
                                                .param("input", message))
                                .call()
                                .content();
        }
}
